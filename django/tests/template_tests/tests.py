# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import sys

from django.contrib.auth.models import Group
from django.core import urlresolvers
from django.template import Context, Engine, TemplateSyntaxError
from django.template.base import UNKNOWN_SOURCE
from django.test import SimpleTestCase, override_settings


class TemplateTests(SimpleTestCase):

    def test_string_origin(self):
        template = Engine().from_string('string template')
        self.assertEqual(template.origin.name, UNKNOWN_SOURCE)
        self.assertEqual(template.origin.loader_name, None)
        self.assertEqual(template.source, 'string template')

    @override_settings(SETTINGS_MODULE=None)
    def test_url_reverse_no_settings_module(self):
        """
        #9005 -- url tag shouldn't require settings.SETTINGS_MODULE to
        be set.
        """
        t = Engine(debug=True).from_string('{% url will_not_match %}')
        c = Context()
        with self.assertRaises(urlresolvers.NoReverseMatch):
            t.render(c)

    def test_url_reverse_view_name(self):
        """
        #19827 -- url tag should keep original strack trace when reraising
        exception.
        """
        t = Engine().from_string('{% url will_not_match %}')
        c = Context()
        try:
            t.render(c)
        except urlresolvers.NoReverseMatch:
            tb = sys.exc_info()[2]
            depth = 0
            while tb.tb_next is not None:
                tb = tb.tb_next
                depth += 1
            self.assertGreater(depth, 5,
                "The traceback context was lost when reraising the traceback. See #19827")

    def test_no_wrapped_exception(self):
        """
        # 16770 -- The template system doesn't wrap exceptions, but annotates
        them.
        """
        engine = Engine(debug=True)
        c = Context({"coconuts": lambda: 42 / 0})
        t = engine.from_string("{{ coconuts }}")

        with self.assertRaises(ZeroDivisionError) as e:
            t.render(c)

        debug = e.exception.template_debug
        self.assertEqual(debug['start'], 0)
        self.assertEqual(debug['end'], 14)

    def test_invalid_block_suggestion(self):
        """
        #7876 -- Error messages should include the unexpected block name.
        """
        engine = Engine()

        with self.assertRaises(TemplateSyntaxError) as e:
            engine.from_string("{% if 1 %}lala{% endblock %}{% endif %}")

        self.assertEqual(
            e.exception.args[0],
            "Invalid block tag: 'endblock', expected 'elif', 'else' or 'endif'",
        )

    def test_compile_filter_expression_error(self):
        """
        19819 -- Make sure the correct token is highlighted for
        FilterExpression errors.
        """
        engine = Engine(debug=True)
        msg = "Could not parse the remainder: '@bar' from 'foo@bar'"

        with self.assertRaisesMessage(TemplateSyntaxError, msg) as e:
            engine.from_string("{% if 1 %}{{ foo@bar }}{% endif %}")

        debug = e.exception.template_debug
        self.assertEqual((debug['start'], debug['end']), (10, 23))
        self.assertEqual((debug['during']), '{{ foo@bar }}')

    def test_compile_tag_error(self):
        """
        Errors raised while compiling nodes should include the token
        information.
        """
        engine = Engine(
            debug=True,
            libraries={'bad_tag': 'template_tests.templatetags.bad_tag'},
        )
        with self.assertRaises(RuntimeError) as e:
            engine.from_string("{% load bad_tag %}{% badtag %}")
        self.assertEqual(e.exception.template_debug['during'], '{% badtag %}')

    def test_super_errors(self):
        """
        #18169 -- NoReverseMatch should not be silence in block.super.
        """
        engine = Engine(app_dirs=True)
        t = engine.get_template('included_content.html')
        with self.assertRaises(urlresolvers.NoReverseMatch):
            t.render(Context())

    def test_debug_tag_non_ascii(self):
        """
        #23060 -- Test non-ASCII model representation in debug output.
        """
        group = Group(name="清風")
        c1 = Context({"objs": [group]})
        t1 = Engine().from_string('{% debug %}')
        self.assertIn("清風", t1.render(c1))

    def test_extends_generic_template(self):
        """
        #24338 -- Allow extending django.template.backends.django.Template
        objects.
        """
        engine = Engine()
        parent = engine.from_string('{% block content %}parent{% endblock %}')
        child = engine.from_string(
            '{% extends parent %}{% block content %}child{% endblock %}')
        self.assertEqual(child.render(Context({'parent': parent})), 'child')
