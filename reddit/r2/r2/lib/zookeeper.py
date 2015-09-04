# The contents of this file are subject to the Common Public Attribution
# License Version 1.0. (the "License"); you may not use this file except in
# compliance with the License. You may obtain a copy of the License at
# http://code.reddit.com/LICENSE. The License is based on the Mozilla Public
# License Version 1.1, but Sections 14 and 15 have been added to cover use of
# software over a computer network and provide for limited attribution for the
# Original Developer. In addition, Exhibit A has been modified to be consistent
# with Exhibit B.
#
# Software distributed under the License is distributed on an "AS IS" basis,
# WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
# the specific language governing rights and limitations under the License.
#
# The Original Code is reddit.
#
# The Original Developer is the Initial Developer.  The Initial Developer of
# the Original Code is reddit Inc.
#
# All portions of the code written by reddit are Copyright (c) 2006-2015 reddit
# Inc. All Rights Reserved.
###############################################################################

import os
import json
import urllib
import functools
import zlib

from kazoo.client import KazooClient
from kazoo.security import make_digest_acl
from kazoo.exceptions import NoNodeException

from r2.lib import hooks


def connect_to_zookeeper(hostlist, credentials):
    """Create a connection to the ZooKeeper ensemble.

    If authentication credentials are provided (as a two-tuple: username,
    password), we will ensure that they are provided to the server whenever we
    establish a connection.

    """

    client = KazooClient(hostlist,
                         timeout=5,
                         max_retries=3,
                         auth_data=[("digest", ":".join(credentials))])

    # convenient helper function for making credentials
    client.make_acl = functools.partial(make_digest_acl, *credentials)

    client.start()
    return client


class LiveConfig(object):
    """A read-only dictionary view of configuration retrieved from ZooKeeper.

    The data will be parsed using the given configuration specs, exactly like
    the ini file based configuration. When data is changed in ZooKeeper, the
    data in this view will automatically update.

    """
    def __init__(self, client, key):
        self.data = {}

        @client.DataWatch(key)
        def watcher(data, stat):
            if data.startswith("gzip"):
                data = zlib.decompress(data[len("gzip"):])
            self.data = json.loads(data)
            hooks.get_hook("worker.live_config.update").call()

    def __getitem__(self, key):
        return self.data[key]

    def get(self, key, default=None):
        return self.data.get(key, default)

    def __repr__(self):
        return "<LiveConfig %r>" % self.data


class LiveList(object):
    """A mutable set shared by all apps and backed by ZooKeeper."""
    def __init__(self, client, root, map_fn=None, reduce_fn=lambda L: L,
                 watch=True):
        self.client = client
        self.root = root
        self.map_fn = map_fn
        self.reduce_fn = reduce_fn
        self.is_watching = watch

        acl = [self.client.make_acl(read=True, create=True, delete=True)]
        self.client.ensure_path(self.root, acl)

        if watch:
            self.data = []

            @client.ChildrenWatch(root)
            def watcher(children):
                self.data = self._normalize_children(children, reduce=True)

    def _nodepath(self, item):
        escaped = urllib.quote(str(item), safe=":")
        return os.path.join(self.root, escaped)

    def _normalize_children(self, children, reduce):
        unquoted = (urllib.unquote(c) for c in children)
        mapped = map(self.map_fn, unquoted)

        if reduce:
            return list(self.reduce_fn(mapped))
        else:
            return list(mapped)

    def add(self, item):
        path = self._nodepath(item)
        self.client.ensure_path(path)

    def remove(self, item):
        path = self._nodepath(item)

        try:
            self.client.delete(path)
        except NoNodeException:
            raise ValueError("not in list")

    def get(self, reduce=True):
        children = self.client.get_children(self.root)
        return self._normalize_children(children, reduce)

    def __iter__(self):
        if not self.is_watching:
            raise NotImplementedError()
        return iter(self.data)

    def __len__(self):
        if not self.is_watching:
            raise NotImplementedError()
        return len(self.data)

    def __repr__(self):
        return "<LiveList %r (%s)>" % (self.data,
                                       "push" if self.is_watching else "pull")
