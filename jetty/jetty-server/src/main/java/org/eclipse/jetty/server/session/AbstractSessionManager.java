//
//  ========================================================================
//  Copyright (c) 1995-2015 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.server.session;

import static java.lang.Math.round;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionContext;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.ManagedOperation;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.statistic.CounterStatistic;
import org.eclipse.jetty.util.statistic.SampleStatistic;

/**
 * An Abstract implementation of SessionManager.
 * <p>
 * The partial implementation of SessionManager interface provides the majority of the handling required to implement a
 * SessionManager. Concrete implementations of SessionManager based on AbstractSessionManager need only implement the
 * newSession method to return a specialized version of the Session inner class that provides an attribute Map.
 */
@SuppressWarnings("deprecation")
@ManagedObject("Abstract Session Manager")
public abstract class AbstractSessionManager extends ContainerLifeCycle implements SessionManager
{
    final static Logger __log = SessionHandler.LOG;

    public Set<SessionTrackingMode> __defaultSessionTrackingModes =
        Collections.unmodifiableSet(
            new HashSet<SessionTrackingMode>(
                    Arrays.asList(new SessionTrackingMode[]{SessionTrackingMode.COOKIE,SessionTrackingMode.URL})));

    

    /* ------------------------------------------------------------ */
    public final static int __distantFuture=60*60*24*7*52*20;

    static final HttpSessionContext __nullSessionContext=new HttpSessionContext()
    {
        @Override
        public HttpSession getSession(String sessionId)
        {
            return null;
        }

        @Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public Enumeration getIds()
        {
            return Collections.enumeration(Collections.EMPTY_LIST);
        }
    };

    private boolean _usingCookies=true;

    /* ------------------------------------------------------------ */
    // Setting of max inactive interval for new sessions
    // -1 means no timeout
    protected int _dftMaxIdleSecs=-1;
    protected SessionHandler _sessionHandler;
    protected boolean _httpOnly=false;
    protected SessionIdManager _sessionIdManager;
    protected boolean _secureCookies=false;
    protected boolean _secureRequestOnly=true;

    protected final List<HttpSessionAttributeListener> _sessionAttributeListeners = new CopyOnWriteArrayList<HttpSessionAttributeListener>();
    protected final List<HttpSessionListener> _sessionListeners= new CopyOnWriteArrayList<HttpSessionListener>();
    protected final List<HttpSessionIdListener> _sessionIdListeners = new CopyOnWriteArrayList<HttpSessionIdListener>();

    protected ClassLoader _loader;
    protected ContextHandler.Context _context;
    protected String _sessionCookie=__DefaultSessionCookie;
    protected String _sessionIdPathParameterName = __DefaultSessionIdPathParameterName;
    protected String _sessionIdPathParameterNamePrefix =";"+ _sessionIdPathParameterName +"=";
    protected String _sessionDomain;
    protected String _sessionPath;
    protected int _maxCookieAge=-1;
    protected int _refreshCookieAge;
    protected boolean _nodeIdInSessionId;
    protected boolean _checkingRemoteSessionIdEncoding;
    protected String _sessionComment;

    public Set<SessionTrackingMode> _sessionTrackingModes;

    private boolean _usingURLs;

    protected final CounterStatistic _sessionsStats = new CounterStatistic();
    protected final SampleStatistic _sessionTimeStats = new SampleStatistic();


    /* ------------------------------------------------------------ */
    public AbstractSessionManager()
    {
        setSessionTrackingModes(__defaultSessionTrackingModes);
    }

    /* ------------------------------------------------------------ */
    public ContextHandler.Context getContext()
    {
        return _context;
    }

    /* ------------------------------------------------------------ */
    public ContextHandler getContextHandler()
    {
        return _context.getContextHandler();
    }

    @ManagedAttribute("path of the session cookie, or null for default")
    public String getSessionPath()
    {
        return _sessionPath;
    }

    @ManagedAttribute("if greater the zero, the time in seconds a session cookie will last for")
    public int getMaxCookieAge()
    {
        return _maxCookieAge;
    }

    /* ------------------------------------------------------------ */
    @Override
    public HttpCookie access(HttpSession session,boolean secure)
    {
        long now=System.currentTimeMillis();

        AbstractSession s = ((SessionIf)session).getSession();

       if (s.access(now))
       {
            // Do we need to refresh the cookie?
            if (isUsingCookies() &&
                (s.isIdChanged() ||
                (getSessionCookieConfig().getMaxAge()>0 && getRefreshCookieAge()>0 && ((now-s.getCookieSetTime())/1000>getRefreshCookieAge()))
                )
               )
            {
                HttpCookie cookie=getSessionCookie(session,_context==null?"/":(_context.getContextPath()),secure);
                s.cookieSet();
                s.setIdChanged(false);
                return cookie;
            }
        }
        return null;
    }

    /* ------------------------------------------------------------ */
    @Override
    public void addEventListener(EventListener listener)
    {
        if (listener instanceof HttpSessionAttributeListener)
            _sessionAttributeListeners.add((HttpSessionAttributeListener)listener);
        if (listener instanceof HttpSessionListener)
            _sessionListeners.add((HttpSessionListener)listener);
        if (listener instanceof HttpSessionIdListener)
            _sessionIdListeners.add((HttpSessionIdListener)listener);
        addBean(listener,false);
    }

    /* ------------------------------------------------------------ */
    @Override
    public void clearEventListeners()
    {
        for (EventListener e :getBeans(EventListener.class))
            removeBean(e);
        _sessionAttributeListeners.clear();
        _sessionListeners.clear();
        _sessionIdListeners.clear();
    }

    /* ------------------------------------------------------------ */
    @Override
    public void complete(HttpSession session)
    {
        AbstractSession s = ((SessionIf)session).getSession();
        s.complete();
    }

    /* ------------------------------------------------------------ */
    @Override
    public void doStart() throws Exception
    {
        _context=ContextHandler.getCurrentContext();
        _loader=Thread.currentThread().getContextClassLoader();

        final Server server=getSessionHandler().getServer();
        synchronized (server)
        {
            if (_sessionIdManager==null)
            {
                _sessionIdManager=server.getSessionIdManager();
                if (_sessionIdManager==null)
                {
                    //create a default SessionIdManager and set it as the shared
                    //SessionIdManager for the Server, being careful NOT to use
                    //the webapp context's classloader, otherwise if the context
                    //is stopped, the classloader is leaked.
                    ClassLoader serverLoader = server.getClass().getClassLoader();
                    try
                    {
                        Thread.currentThread().setContextClassLoader(serverLoader);
                        _sessionIdManager=new HashSessionIdManager();
                        server.setSessionIdManager(_sessionIdManager);
                        server.manage(_sessionIdManager);
                        _sessionIdManager.start();
                    }
                    finally
                    {
                        Thread.currentThread().setContextClassLoader(_loader);
                    }
                }

                // server session id is never managed by this manager
                addBean(_sessionIdManager,false);
            }
        }
        

        // Look for a session cookie name
        if (_context!=null)
        {
            String tmp=_context.getInitParameter(SessionManager.__SessionCookieProperty);
            if (tmp!=null)
                _sessionCookie=tmp;

            tmp=_context.getInitParameter(SessionManager.__SessionIdPathParameterNameProperty);
            if (tmp!=null)
                setSessionIdPathParameterName(tmp);

            // set up the max session cookie age if it isn't already
            if (_maxCookieAge==-1)
            {
                tmp=_context.getInitParameter(SessionManager.__MaxAgeProperty);
                if (tmp!=null)
                    _maxCookieAge=Integer.parseInt(tmp.trim());
            }

            // set up the session domain if it isn't already
            if (_sessionDomain==null)
                _sessionDomain=_context.getInitParameter(SessionManager.__SessionDomainProperty);

            // set up the sessionPath if it isn't already
            if (_sessionPath==null)
                _sessionPath=_context.getInitParameter(SessionManager.__SessionPathProperty);

            tmp=_context.getInitParameter(SessionManager.__CheckRemoteSessionEncoding);
            if (tmp!=null)
                _checkingRemoteSessionIdEncoding=Boolean.parseBoolean(tmp);
        }

        super.doStart();
    }

    /* ------------------------------------------------------------ */
    @Override
    public void doStop() throws Exception
    {
        super.doStop();

        shutdownSessions();

        _loader=null;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the httpOnly.
     */
    @Override
    @ManagedAttribute("true if cookies use the http only flag")
    public boolean getHttpOnly()
    {
        return _httpOnly;
    }

    /* ------------------------------------------------------------ */
    @Override
    public HttpSession getHttpSession(String nodeId)
    {
        String cluster_id = getSessionIdManager().getClusterId(nodeId);

        AbstractSession session = getSession(cluster_id);
        if (session!=null && !session.getNodeId().equals(nodeId))
            session.setIdChanged(true);
        return session;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the SessionIdManager used for cross context session management
     */
    @Override
    @ManagedAttribute("Session ID Manager")
    public SessionIdManager getSessionIdManager()
    {
        return _sessionIdManager;
    }


    /* ------------------------------------------------------------ */
    /**
     * @return seconds
     */
    @Override
    @ManagedAttribute("defailt maximum time a session may be idle for (in s)")
    public int getMaxInactiveInterval()
    {
        return _dftMaxIdleSecs;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return maximum number of sessions
     */
    @ManagedAttribute("maximum number of simultaneous sessions")
    public int getSessionsMax()
    {
        return (int)_sessionsStats.getMax();
    }

    /* ------------------------------------------------------------ */
    /**
     * @return total number of sessions
     */
    @ManagedAttribute("total number of sessions")
    public int getSessionsTotal()
    {
        return (int)_sessionsStats.getTotal();
    }

    /* ------------------------------------------------------------ */
    @ManagedAttribute("time before a session cookie is re-set (in s)")
    public int getRefreshCookieAge()
    {
        return _refreshCookieAge;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return same as SessionCookieConfig.getSecure(). If true, session
     * cookies are ALWAYS marked as secure. If false, a session cookie is
     * ONLY marked as secure if _secureRequestOnly == true and it is a HTTPS request.
     */
    @ManagedAttribute("if true, secure cookie flag is set on session cookies")
    public boolean getSecureCookies()
    {
        return _secureCookies;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return true if session cookie is to be marked as secure only on HTTPS requests
     */
    public boolean isSecureRequestOnly()
    {
        return _secureRequestOnly;
    }


    /* ------------------------------------------------------------ */
    /**
     * HTTPS request. Can be overridden by setting SessionCookieConfig.setSecure(true),
     * in which case the session cookie will be marked as secure on both HTTPS and HTTP.
     * @param secureRequestOnly true to set Session Cookie Config as secure
     */
    public void setSecureRequestOnly(boolean secureRequestOnly)
    {
        _secureRequestOnly = secureRequestOnly;
    }

    /* ------------------------------------------------------------ */
    @ManagedAttribute("the set session cookie")
    public String getSessionCookie()
    {
        return _sessionCookie;
    }

    /* ------------------------------------------------------------ */
    /**
     * A sessioncookie is marked as secure IFF any of the following conditions are true:
     * <ol>
     * <li>SessionCookieConfig.setSecure == true</li>
     * <li>SessionCookieConfig.setSecure == false &amp;&amp; _secureRequestOnly==true &amp;&amp; request is HTTPS</li>
     * </ol>
     * According to SessionCookieConfig javadoc, case 1 can be used when:
     * "... even though the request that initiated the session came over HTTP,
     * is to support a topology where the web container is front-ended by an
     * SSL offloading load balancer. In this case, the traffic between the client
     * and the load balancer will be over HTTPS, whereas the traffic between the
     * load balancer and the web container will be over HTTP."
     * <p>
     * For case 2, you can use _secureRequestOnly to determine if you want the
     * Servlet Spec 3.0  default behavior when SessionCookieConfig.setSecure==false,
     * which is:
     * <cite>
     * "they shall be marked as secure only if the request that initiated the
     * corresponding session was also secure"
     * </cite>
     * <p>
     * The default for _secureRequestOnly is true, which gives the above behavior. If
     * you set it to false, then a session cookie is NEVER marked as secure, even if
     * the initiating request was secure.
     *
     * @see org.eclipse.jetty.server.SessionManager#getSessionCookie(javax.servlet.http.HttpSession, java.lang.String, boolean)
     */
    @Override
    public HttpCookie getSessionCookie(HttpSession session, String contextPath, boolean requestIsSecure)
    {
        if (isUsingCookies())
        {
            String sessionPath = (_cookieConfig.getPath()==null) ? contextPath : _cookieConfig.getPath();
            sessionPath = (sessionPath==null||sessionPath.length()==0) ? "/" : sessionPath;
            String id = getNodeId(session);
            HttpCookie cookie = null;
            if (_sessionComment == null)
            {
                cookie = new HttpCookie(
                                        _cookieConfig.getName(),
                                        id,
                                        _cookieConfig.getDomain(),
                                        sessionPath,
                                        _cookieConfig.getMaxAge(),
                                        _cookieConfig.isHttpOnly(),
                                        _cookieConfig.isSecure() || (isSecureRequestOnly() && requestIsSecure));
            }
            else
            {
                cookie = new HttpCookie(
                                        _cookieConfig.getName(),
                                        id,
                                        _cookieConfig.getDomain(),
                                        sessionPath,
                                        _cookieConfig.getMaxAge(),
                                        _cookieConfig.isHttpOnly(),
                                        _cookieConfig.isSecure() || (isSecureRequestOnly() && requestIsSecure),
                                        _sessionComment,
                                        1);
            }

            return cookie;
        }
        return null;
    }

    @ManagedAttribute("domain of the session cookie, or null for the default")
    public String getSessionDomain()
    {
        return _sessionDomain;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the sessionHandler.
     */
    public SessionHandler getSessionHandler()
    {
        return _sessionHandler;
    }

    /* ------------------------------------------------------------ */
    @ManagedAttribute("number of currently active sessions")
    public int getSessions()
    {
        return (int)_sessionsStats.getCurrent();
    }

    /* ------------------------------------------------------------ */
    @Override
    @ManagedAttribute("name of use for URL session tracking")
    public String getSessionIdPathParameterName()
    {
        return _sessionIdPathParameterName;
    }

    /* ------------------------------------------------------------ */
    @Override
    public String getSessionIdPathParameterNamePrefix()
    {
        return _sessionIdPathParameterNamePrefix;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the usingCookies.
     */
    @Override
    public boolean isUsingCookies()
    {
        return _usingCookies;
    }

    /* ------------------------------------------------------------ */
    @Override
    public boolean isValid(HttpSession session)
    {
        AbstractSession s = ((SessionIf)session).getSession();
        return s.isValid();
    }

    /* ------------------------------------------------------------ */
    @Override
    public String getClusterId(HttpSession session)
    {
        AbstractSession s = ((SessionIf)session).getSession();
        return s.getClusterId();
    }

    /* ------------------------------------------------------------ */
    @Override
    public String getNodeId(HttpSession session)
    {
        AbstractSession s = ((SessionIf)session).getSession();
        return s.getNodeId();
    }

    /* ------------------------------------------------------------ */
    /**
     * Create a new HttpSession for a request
     */
    @Override
    public HttpSession newHttpSession(HttpServletRequest request)
    {
        AbstractSession session=newSession(request);
        session.setMaxInactiveInterval(_dftMaxIdleSecs);
        if (request.isSecure())
            session.setAttribute(AbstractSession.SESSION_CREATED_SECURE, Boolean.TRUE);
        addSession(session,true);
        return session;
    }

    /* ------------------------------------------------------------ */
    @Override
    public void removeEventListener(EventListener listener)
    {
        if (listener instanceof HttpSessionAttributeListener)
            _sessionAttributeListeners.remove(listener);
        if (listener instanceof HttpSessionListener)
            _sessionListeners.remove(listener);
        if (listener instanceof HttpSessionIdListener)
            _sessionIdListeners.remove(listener);
        removeBean(listener);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Reset statistics values
     */
    @ManagedOperation(value="reset statistics", impact="ACTION")
    public void statsReset()
    {
        _sessionsStats.reset(getSessions());
        _sessionTimeStats.reset();
    }

    /* ------------------------------------------------------------ */
    /**
     * @param httpOnly
     *            The httpOnly to set.
     */
    public void setHttpOnly(boolean httpOnly)
    {
        _httpOnly=httpOnly;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param metaManager The metaManager used for cross context session management.
     */
    @Override
    public void setSessionIdManager(SessionIdManager metaManager)
    {
        updateBean(_sessionIdManager, metaManager);
        _sessionIdManager=metaManager;
    }

    /* ------------------------------------------------------------ */
    @Override
    public void setMaxInactiveInterval(int seconds)
    {
        _dftMaxIdleSecs=seconds;
    }

    /* ------------------------------------------------------------ */
    public void setRefreshCookieAge(int ageInSeconds)
    {
        _refreshCookieAge=ageInSeconds;
    }

    /* ------------------------------------------------------------ */
    public void setSessionCookie(String cookieName)
    {
        _sessionCookie=cookieName;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param sessionHandler
     *            The sessionHandler to set.
     */
    @Override
    public void setSessionHandler(SessionHandler sessionHandler)
    {
        _sessionHandler=sessionHandler;
    }


    /* ------------------------------------------------------------ */
    @Override
    public void setSessionIdPathParameterName(String param)
    {
        _sessionIdPathParameterName =(param==null||"none".equals(param))?null:param;
        _sessionIdPathParameterNamePrefix =(param==null||"none".equals(param))?null:(";"+ _sessionIdPathParameterName +"=");
    }
    /* ------------------------------------------------------------ */
    /**
     * @param usingCookies
     *            The usingCookies to set.
     */
    public void setUsingCookies(boolean usingCookies)
    {
        _usingCookies=usingCookies;
    }


    protected abstract void addSession(AbstractSession session);

    /* ------------------------------------------------------------ */
    /**
     * Add the session Registers the session with this manager and registers the
     * session ID with the sessionIDManager;
     * @param session the session
     * @param created true if session was created
     */
    protected void addSession(AbstractSession session, boolean created)
    {
        synchronized (_sessionIdManager)
        {
            _sessionIdManager.addSession(session);
            addSession(session);
        }

        if (created)
        {
            _sessionsStats.increment();
            if (_sessionListeners!=null)
            {
                HttpSessionEvent event=new HttpSessionEvent(session);
                for (HttpSessionListener listener : _sessionListeners)
                    listener.sessionCreated(event);
            }
        }
    }

    /* ------------------------------------------------------------ */
    /**
     * Get a known existing session
     * @param idInCluster The session ID in the cluster, stripped of any worker name.
     * @return A Session or null if none exists.
     */
    public abstract AbstractSession getSession(String idInCluster);

    /**
     * Prepare sessions for session manager shutdown
     * 
     * @throws Exception if unable to shutdown sesssions
     */
    protected abstract void shutdownSessions() throws Exception;


    /* ------------------------------------------------------------ */
    /**
     * Create a new session instance
     * @param request the request to build the session from
     * @return the new session
     */
    protected abstract AbstractSession newSession(HttpServletRequest request);


    /* ------------------------------------------------------------ */
    /**
     * @return true if the cluster node id (worker id) is returned as part of the session id by {@link HttpSession#getId()}. Default is false.
     */
    public boolean isNodeIdInSessionId()
    {
        return _nodeIdInSessionId;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param nodeIdInSessionId true if the cluster node id (worker id) will be returned as part of the session id by {@link HttpSession#getId()}. Default is false.
     */
    public void setNodeIdInSessionId(boolean nodeIdInSessionId)
    {
        _nodeIdInSessionId=nodeIdInSessionId;
    }

    /* ------------------------------------------------------------ */
    /** Remove session from manager
     * @param session The session to remove
     * @param invalidate True if {@link HttpSessionListener#sessionDestroyed(HttpSessionEvent)} and
     * {@link SessionIdManager#invalidateAll(String)} should be called.
     */
    public void removeSession(HttpSession session, boolean invalidate)
    {
        AbstractSession s = ((SessionIf)session).getSession();
        removeSession(s,invalidate);
    }

    /* ------------------------------------------------------------ */
    /** 
     * Remove session from manager
     * @param session The session to remove
     * @param invalidate True if {@link HttpSessionListener#sessionDestroyed(HttpSessionEvent)} and
     * {@link SessionIdManager#invalidateAll(String)} should be called.
     * @return if the session was removed 
     */
    public boolean removeSession(AbstractSession session, boolean invalidate)
    {
        // Remove session from context and global maps
        boolean removed = removeSession(session.getClusterId());

        if (removed)
        {
            _sessionsStats.decrement();
            _sessionTimeStats.set(round((System.currentTimeMillis() - session.getCreationTime())/1000.0));

            // Remove session from all context and global id maps
            _sessionIdManager.removeSession(session);
            if (invalidate)
                _sessionIdManager.invalidateAll(session.getClusterId());

            if (invalidate && _sessionListeners!=null)
            {
                HttpSessionEvent event=new HttpSessionEvent(session);      
                for (int i = _sessionListeners.size()-1; i>=0; i--)
                {
                    _sessionListeners.get(i).sessionDestroyed(event);
                }
            }
        }
        
        return removed;
    }

    /* ------------------------------------------------------------ */
    protected abstract boolean removeSession(String idInCluster);

    /* ------------------------------------------------------------ */
    /**
     * @return maximum amount of time session remained valid
     */
    @ManagedAttribute("maximum amount of time sessions have remained active (in s)")
    public long getSessionTimeMax()
    {
        return _sessionTimeStats.getMax();
    }

    /* ------------------------------------------------------------ */
    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes()
    {
        return __defaultSessionTrackingModes;
    }

    /* ------------------------------------------------------------ */
    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes()
    {
        return Collections.unmodifiableSet(_sessionTrackingModes);
    }

    /* ------------------------------------------------------------ */
    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes)
    {
        _sessionTrackingModes=new HashSet<SessionTrackingMode>(sessionTrackingModes);
        _usingCookies=_sessionTrackingModes.contains(SessionTrackingMode.COOKIE);
        _usingURLs=_sessionTrackingModes.contains(SessionTrackingMode.URL);
    }

    /* ------------------------------------------------------------ */
    @Override
    public boolean isUsingURLs()
    {
        return _usingURLs;
    }

    /* ------------------------------------------------------------ */
    @Override
    public SessionCookieConfig getSessionCookieConfig()
    {
        return _cookieConfig;
    }

    /* ------------------------------------------------------------ */
    private SessionCookieConfig _cookieConfig =
        new CookieConfig();


    /* ------------------------------------------------------------ */
    /**
     * @return total amount of time all sessions remained valid
     */
    @ManagedAttribute("total time sessions have remained valid")
    public long getSessionTimeTotal()
    {
        return _sessionTimeStats.getTotal();
    }

    /* ------------------------------------------------------------ */
    /**
     * @return mean amount of time session remained valid
     */
    @ManagedAttribute("mean time sessions remain valid (in s)")
    public double getSessionTimeMean()
    {
        return _sessionTimeStats.getMean();
    }

    /* ------------------------------------------------------------ */
    /**
     * @return standard deviation of amount of time session remained valid
     */
    @ManagedAttribute("standard deviation a session remained valid (in s)")
    public double getSessionTimeStdDev()
    {
        return _sessionTimeStats.getStdDev();
    }

    /* ------------------------------------------------------------ */
    /**
     * @see org.eclipse.jetty.server.SessionManager#isCheckingRemoteSessionIdEncoding()
     */
    @Override
    @ManagedAttribute("check remote session id encoding")
    public boolean isCheckingRemoteSessionIdEncoding()
    {
        return _checkingRemoteSessionIdEncoding;
    }

    /* ------------------------------------------------------------ */
    /**
     * @see org.eclipse.jetty.server.SessionManager#setCheckingRemoteSessionIdEncoding(boolean)
     */
    @Override
    public void setCheckingRemoteSessionIdEncoding(boolean remote)
    {
        _checkingRemoteSessionIdEncoding=remote;
    }
    
    
    /* ------------------------------------------------------------ */
    /**
     * Tell the HttpSessionIdListeners the id changed.
     * NOTE: this method must be called LAST in subclass overrides, after the session has been updated
     * with the new id.
     * @see org.eclipse.jetty.server.SessionManager#renewSessionId(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void renewSessionId(String oldClusterId, String oldNodeId, String newClusterId, String newNodeId)
    {
        if (!_sessionIdListeners.isEmpty())
        {
            AbstractSession session = getSession(newClusterId);
            HttpSessionEvent event = new HttpSessionEvent(session);
            for (HttpSessionIdListener l:_sessionIdListeners)
            {
                l.sessionIdChanged(event, oldClusterId);
            }
        }

    }

    /**
     * CookieConfig
     * 
     * Implementation of the javax.servlet.SessionCookieConfig.
     */
    public final class CookieConfig implements SessionCookieConfig
    {
        @Override
        public String getComment()
        {
            return _sessionComment;
        }

        @Override
        public String getDomain()
        {
            return _sessionDomain;
        }

        @Override
        public int getMaxAge()
        {
            return _maxCookieAge;
        }

        @Override
        public String getName()
        {
            return _sessionCookie;
        }

        @Override
        public String getPath()
        {
            return _sessionPath;
        }

        @Override
        public boolean isHttpOnly()
        {
            return _httpOnly;
        }

        @Override
        public boolean isSecure()
        {
            return _secureCookies;
        }

        @Override
        public void setComment(String comment)
        {  
            if (_context != null && _context.getContextHandler().isAvailable())
                throw new IllegalStateException("CookieConfig cannot be set after ServletContext is started");
            _sessionComment = comment;
        }

        @Override
        public void setDomain(String domain)
        {
            if (_context != null && _context.getContextHandler().isAvailable())
                throw new IllegalStateException("CookieConfig cannot be set after ServletContext is started");
            _sessionDomain=domain;
        }

        @Override
        public void setHttpOnly(boolean httpOnly)
        {   
            if (_context != null && _context.getContextHandler().isAvailable())
                throw new IllegalStateException("CookieConfig cannot be set after ServletContext is started");
            _httpOnly=httpOnly;
        }

        @Override
        public void setMaxAge(int maxAge)
        {               
            if (_context != null && _context.getContextHandler().isAvailable())
                throw new IllegalStateException("CookieConfig cannot be set after ServletContext is started");
            _maxCookieAge=maxAge;
        }

        @Override
        public void setName(String name)
        {  
                if (_context != null && _context.getContextHandler().isAvailable())
                    throw new IllegalStateException("CookieConfig cannot be set after ServletContext is started");
            _sessionCookie=name;
        }

        @Override
        public void setPath(String path)
        {
            if (_context != null && _context.getContextHandler().isAvailable())
                throw new IllegalStateException("CookieConfig cannot be set after ServletContext is started"); 
            _sessionPath=path;
        }

        @Override
        public void setSecure(boolean secure)
        {
            if (_context != null && _context.getContextHandler().isAvailable())
                throw new IllegalStateException("CookieConfig cannot be set after ServletContext is started");
            _secureCookies=secure;
        }
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /**
     * Interface that any session wrapper should implement so that
     * SessionManager may access the Jetty session implementation.
     *
     */
    public interface SessionIf extends HttpSession
    {
        public AbstractSession getSession();
    }

    public void doSessionAttributeListeners(AbstractSession session, String name, Object old, Object value)
    {
        if (!_sessionAttributeListeners.isEmpty())
        {
            HttpSessionBindingEvent event=new HttpSessionBindingEvent(session,name,old==null?value:old);

            for (HttpSessionAttributeListener l : _sessionAttributeListeners)
            {
                if (old==null)
                    l.attributeAdded(event);
                else if (value==null)
                    l.attributeRemoved(event);
                else
                    l.attributeReplaced(event);
            }
        }
    }

    @Override
    @Deprecated
    public SessionIdManager getMetaManager()
    {
        throw new UnsupportedOperationException();
    }
}
