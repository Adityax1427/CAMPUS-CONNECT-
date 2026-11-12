import { useCallback, useEffect, useMemo, useState } from 'react';
import { authApi, request } from './api';

const categories = ['All events', 'Workshop', 'Social', 'Competition', 'Talk'];
const navItems = [
  { id: 'discover', label: 'Discover', icon: 'compass' },
  { id: 'my-events', label: 'My events', icon: 'calendar' },
  { id: 'clubs', label: 'Clubs', icon: 'users' },
  { id: 'announcements', label: 'Notice board', icon: 'megaphone' },
];

function Icon({ name, size = 18 }) {
  const paths = {
    compass: <><circle cx="12" cy="12" r="9" /><path d="m15.8 8.2-2.5 5.1-5.1 2.5 2.5-5.1 5.1-2.5Z" /></>,
    calendar: <><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M16 3v4M8 3v4M3 11h18" /></>,
    users: <><path d="M16 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2M10 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM20 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" /></>,
    megaphone: <><path d="m3 11 18-5v12L3 13v-2ZM11.6 15.4 13 21l4-1-2.7-5.5M3 11v2a3 3 0 0 0 3 3h1v-5" /></>,
    search: <><circle cx="10.8" cy="10.8" r="6.8" /><path d="m16 16 4.5 4.5" /></>,
    bell: <><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" /></>,
    plus: <><path d="M12 5v14M5 12h14" /></>,
    arrow: <><path d="M5 12h14M13 6l6 6-6 6" /></>,
    pin: <><path d="M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0Z" /><circle cx="12" cy="10" r="2.5" /></>,
    clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
    check: <path d="m5 12 4 4L19 6" />,
    arrowLeft: <><path d="M19 12H5M11 18l-6-6 6-6" /></>,
    close: <><path d="m18 6-12 12M6 6l12 12" /></>,
    shield: <><path d="M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11Z" /><path d="m9 12 2 2 4-4" /></>,
    logout: <><path d="M10 17l5-5-5-5M15 12H3M12 3h6a3 3 0 0 1 3 3v12a3 3 0 0 1-3 3h-6" /></>,
    spark: <><path d="m12 3 1.9 5.8L20 11l-6.1 2.2L12 19l-2-5.8L4 11l6-2.2L12 3ZM19 14l1.2 2.8L23 18l-2.8 1.2L19 22l-1.2-2.8L15 18l2.8-1.2L19 14Z" /></>,
    menu: <><path d="M4 6h16M4 12h16M4 18h16" /></>,
  };
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name]}</svg>;
}

const fallbackImages = {
  Workshop: 'https://images.unsplash.com/photo-1523580494863-6f3031224c94?auto=format&fit=crop&w=960&q=85',
  Social: 'https://images.unsplash.com/photo-1528605248644-14dd04022da1?auto=format&fit=crop&w=960&q=85',
  Competition: 'https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?auto=format&fit=crop&w=960&q=85',
  Talk: 'https://images.unsplash.com/photo-1475721027785-f74eccf877e2?auto=format&fit=crop&w=960&q=85',
};

function formatDay(date) {
  return new Intl.DateTimeFormat('en', { weekday: 'short', month: 'short', day: 'numeric' }).format(new Date(date));
}

function formatTime(date) {
  return new Intl.DateTimeFormat('en', { hour: 'numeric', minute: '2-digit' }).format(new Date(date));
}

function App() {
  const [token, setToken] = useState(() => localStorage.getItem('campusconnect-token'));
  const [user, setUser] = useState(null);
  const [view, setView] = useState('discover');
  const [events, setEvents] = useState([]);
  const [clubs, setClubs] = useState([]);
  const [announcements, setAnnouncements] = useState([]);
  const [registrations, setRegistrations] = useState([]);
  const [category, setCategory] = useState('All events');
  const [query, setQuery] = useState('');
  const [modal, setModal] = useState(false);
  const [editingEvent, setEditingEvent] = useState(null);
  const [announcementModal, setAnnouncementModal] = useState(false);
  const [clubModal, setClubModal] = useState(false);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [mobileNav, setMobileNav] = useState(false);
  const [adminSummary, setAdminSummary] = useState(null);
  const [adminUsers, setAdminUsers] = useState([]);
  const [aiOpen, setAiOpen] = useState(false);
  const [aiMessages, setAiMessages] = useState([
    { role: 'assistant', text: 'I can help you find the best campus events, clubs, and updates for your week.' },
  ]);
  const [aiInput, setAiInput] = useState('');
  const [aiBusy, setAiBusy] = useState(false);

  const refresh = useCallback(async (currentToken = token) => {
    const [eventData, clubData, noticeData] = await Promise.all([
      request('/events'),
      request('/clubs'),
      request('/announcements'),
    ]);
    setEvents(eventData);
    setClubs(clubData);
    setAnnouncements(noticeData);
    if (currentToken) setRegistrations(await request('/events/mine', { token: currentToken }));
  }, [token]);

  useEffect(() => {
    if (!token) return;
    let active = true;
    authApi.me(token).then((profile) => {
      if (!active) return;
      setUser(profile);
      return refresh(token);
    }).catch(() => {
      if (!active) return;
      localStorage.removeItem('campusconnect-token');
      setToken(null);
      setUser(null);
      setError('Your session expired. Please sign in again.');
    });
    return () => { active = false; };
  }, [token, refresh]);

  useEffect(() => {
    if (!notice) return undefined;
    const timeout = window.setTimeout(() => setNotice(''), 3500);
    return () => window.clearTimeout(timeout);
  }, [notice]);

  useEffect(() => {
    if (view !== 'admin' || user?.role !== 'ADMIN') return;
    Promise.all([request('/admin/summary', { token }), request('/admin/users', { token })])
      .then(([summary, users]) => {
        setAdminSummary(summary);
        setAdminUsers(users);
      })
      .catch((issue) => setNotice(issue.message));
  }, [view, token, user]);

  const registeredIds = useMemo(() => new Set(registrations.map((item) => item.eventId)), [registrations]);
  const visibleEvents = useMemo(() => {
    const now = new Date();
    return events.filter((event) => {
      const matchesCategory = category === 'All events' || event.category === category;
      const matchesSearch = `${event.title} ${event.clubName || ''} ${event.venue} ${event.category}`.toLowerCase().includes(query.toLowerCase());
      const isUpcoming = new Date(event.startsAt) >= now;
      const matchesView = view !== 'my-events' || registeredIds.has(event.id);
      return matchesCategory && matchesSearch && isUpcoming && matchesView;
    });
  }, [events, category, query, view, registeredIds]);

  async function handleLogin(email, password, createAccount, name) {
    setBusy(true);
    setError('');
    try {
      const result = createAccount
        ? await authApi.register(name, email, password)
        : await authApi.login(email, password);
      localStorage.setItem('campusconnect-token', result.token);
      setUser(result.user);
      setToken(result.token);
      setNotice(createAccount ? 'Welcome to CampusConnect!' : `Welcome back, ${result.user.name.split(' ')[0]}!`);
    } catch (issue) {
      setError(issue.message);
    } finally {
      setBusy(false);
    }
  }

  async function toggleRegistration(event) {
    const alreadyJoined = registeredIds.has(event.id);
    setBusy(true);
    try {
      await request(`/events/${event.id}/registrations`, {
        token,
        method: alreadyJoined ? 'DELETE' : 'POST',
      });
      await refresh();
      setNotice(alreadyJoined ? 'You left this event.' : 'You’re on the list! See you there.');
    } catch (issue) {
      setNotice(issue.message);
    } finally {
      setBusy(false);
    }
  }

  async function joinClub(club) {
    setBusy(true);
    try {
      await request(`/clubs/${club.id}/join`, { token, method: 'POST' });
      await refresh();
      setNotice(`You’re now a member of ${club.name}.`);
    } catch (issue) {
      setNotice(issue.message);
    } finally {
      setBusy(false);
    }
  }

  async function createEvent(formData) {
    setBusy(true);
    try {
      await request(editingEvent ? `/events/${editingEvent.id}` : '/events', {
        token,
        method: editingEvent ? 'PUT' : 'POST',
        body: JSON.stringify(formData),
      });
      await refresh();
      setModal(false);
      setEditingEvent(null);
      setNotice(editingEvent ? 'Your event has been updated.' : 'Your event is live. Let’s get the campus talking!');
    } catch (issue) {
      setNotice(issue.message);
    } finally {
      setBusy(false);
    }
  }

  async function deleteEvent(event) {
    if (!window.confirm(`Delete “${event.title}”? This also removes its registrations.`)) return;
    try {
      await request(`/events/${event.id}`, { token, method: 'DELETE' });
      await refresh();
      setNotice('Event deleted.');
    } catch (issue) {
      setNotice(issue.message);
    }
  }

  async function createAnnouncement(formData) {
    setBusy(true);
    try {
      await request('/announcements', { token, method: 'POST', body: JSON.stringify(formData) });
      await refresh();
      setAnnouncementModal(false);
      setNotice('Announcement posted to the notice board.');
    } catch (issue) {
      setNotice(issue.message);
    } finally {
      setBusy(false);
    }
  }

  async function createClub(formData) {
    setBusy(true);
    try {
      await request('/clubs', { token, method: 'POST', body: JSON.stringify(formData) });
      await refresh();
      setClubModal(false);
      setNotice('Your new club is ready to meet its people.');
    } catch (issue) {
      setNotice(issue.message);
    } finally {
      setBusy(false);
    }
  }

  async function updateRole(userId, role) {
    try {
      await request(`/admin/users/${userId}/role`, {
        token,
        method: 'PUT',
        body: JSON.stringify({ role }),
      });
      const users = await request('/admin/users', { token });
      setAdminUsers(users);
      setNotice('Account role updated.');
    } catch (issue) {
      setNotice(issue.message);
    }
  }

  async function handleAiAsk(promptOverride) {
    const prompt = (promptOverride ?? aiInput).trim();
    if (!prompt || aiBusy) return;

    setAiMessages((current) => [...current, { role: 'user', text: prompt }]);
    setAiInput('');
    setAiBusy(true);

    try {
      const result = await request('/ai/chat', {
        token,
        method: 'POST',
        body: JSON.stringify({ prompt }),
      });
      setAiMessages((current) => [...current, { role: 'assistant', text: result.answer || 'I can help with that.' }]);
    } catch (issue) {
      setAiMessages((current) => [...current, { role: 'assistant', text: `I’m having trouble answering that right now: ${issue.message}` }]);
    } finally {
      setAiBusy(false);
    }
  }

  function signOut() {
    localStorage.removeItem('campusconnect-token');
    setToken(null);
    setUser(null);
    setEvents([]);
    setRegistrations([]);
    setView('discover');
  }

  if (!user) {
    return <LoginScreen onSubmit={handleLogin} error={error} busy={busy} />;
  }

  const canOrganize = user.role === 'ORGANIZER' || user.role === 'ADMIN';
  const visibleNavItems = [
    ...navItems,
    ...(canOrganize ? [{ id: 'manage', label: 'Manage events', icon: 'calendar' }] : []),
    ...(user.role === 'ADMIN' ? [{ id: 'admin', label: 'Admin panel', icon: 'shield' }] : []),
  ];
  const greetingName = user.name.split(' ')[0];
  const featured = events.find((event) => new Date(event.startsAt) >= new Date());
  const memberCount = clubs.reduce((total, club) => total + club.memberCount, 0);

  return (
    <div className="app-shell">
      <aside className={`sidebar ${mobileNav ? 'sidebar-open' : ''}`}>
        <a className="brand" href="#" onClick={(event) => { event.preventDefault(); setView('discover'); }}>
          <span className="brand-mark"><Icon name="spark" size={20} /></span>
          <span>campus<span className="brand-light">connect</span></span>
        </a>
        <div className="campus-switcher">
          <div className="campus-avatar">N</div>
          <div><strong>Northstar University</strong><small>Student community</small></div>
          <span className="switcher-arrow">⌄</span>
        </div>
        <p className="nav-caption">MENU</p>
        <nav className="main-nav" aria-label="Main navigation">
          {visibleNavItems.map((item) => (
            <button key={item.id} className={`nav-link ${view === item.id ? 'nav-active' : ''}`} onClick={() => { setView(item.id); setMobileNav(false); }}>
              <Icon name={item.icon} size={18} /><span>{item.label}</span>
              {item.id === 'my-events' && registrations.length > 0 && <span className="nav-count">{registrations.length}</span>}
            </button>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="sidebar-tip">
            <span className="tip-icon"><Icon name="spark" size={18} /></span>
            <strong>Good things happen together.</strong>
            <p>Find your people. Try something new.</p>
            <button onClick={() => setView('clubs')}>Explore clubs <Icon name="arrow" size={14} /></button>
          </div>
          <div className="profile-row">
            <div className="profile-avatar">{user.name.split(' ').map((part) => part[0]).slice(0, 2).join('')}</div>
            <div className="profile-copy"><strong>{user.name}</strong><small>{user.role.toLowerCase()}</small></div>
            <button className="icon-button logout-button" title="Sign out" onClick={signOut}><Icon name="logout" size={17} /></button>
          </div>
        </div>
      </aside>

      <main className="main-area">
        <header className="topbar">
          <button className="icon-button mobile-menu" onClick={() => setMobileNav(!mobileNav)} aria-label="Toggle navigation"><Icon name="menu" /></button>
          <div className="breadcrumbs">Campus <span>/</span> <strong>{visibleNavItems.find((item) => item.id === view)?.label || 'Discover'}</strong></div>
          <div className="topbar-actions">
            <label className="global-search">
              <Icon name="search" size={17} />
              <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search events, clubs..." />
              <kbd>⌘ K</kbd>
            </label>
            <button className="icon-button notification-button" title="Announcements" onClick={() => setView('announcements')}>
              <Icon name="bell" size={19} /><span className="notification-dot" />
            </button>
            <button className="button button-outline ai-toggle" onClick={() => setAiOpen((open) => !open)}><Icon name="spark" size={15} />Campus AI</button>
            {canOrganize && <button className="button button-dark create-button" onClick={() => setModal(true)}><Icon name="plus" size={16} /> Create event</button>}
          </div>
        </header>

        <div className="page-content">
          {view === 'discover' && (
            <>
              <section className="welcome-row">
                <div><div className="eyebrow"><span className="eyebrow-dot" /> YOUR CAMPUS, YOUR COMMUNITY</div><h1>Make this semester <span>count, {greetingName}.</span></h1><p>Big ideas, new friends, and stories worth telling. It all starts here.</p></div>
                <div className="welcome-date"><span>{new Intl.DateTimeFormat('en', { weekday: 'long' }).format(new Date())}</span><strong>{new Intl.DateTimeFormat('en', { month: 'long', day: 'numeric' }).format(new Date())}</strong></div>
              </section>

              {featured && (
                <section className="hero-event" style={{ backgroundImage: `linear-gradient(90deg, rgba(22,28,47,.95) 0%, rgba(22,28,47,.76) 42%, rgba(22,28,47,.06) 100%), url("${featured.imageUrl || fallbackImages[featured.category] || fallbackImages.Social}")` }}>
                  <div className="hero-copy">
                    <span className="hero-kicker"><span className="live-dot" /> HAPPENING ON CAMPUS</span>
                    <h2>{featured.title}</h2><p>{featured.description}</p>
                    <div className="hero-meta"><span><Icon name="calendar" size={15} />{formatDay(featured.startsAt)}</span><span><Icon name="pin" size={15} />{featured.venue}</span></div>
                    <button className="button button-lime" onClick={() => toggleRegistration(featured)} disabled={busy}>
                      {registeredIds.has(featured.id) ? 'You’re going' : 'Save your spot'} <Icon name="arrow" size={16} />
                    </button>
                  </div>
                  <div className="hero-sticker"><span>MAKE</span><strong>MEMORIES</strong><span>NOT PLANS ✳</span></div>
                </section>
              )}

              <section className="stats-row" aria-label="Campus community stats">
                <StatCard icon="calendar" label="Events this week" value={events.filter((event) => {
                  const date = new Date(event.startsAt); const nextWeek = Date.now() + 7 * 86400000;
                  return date >= new Date() && date.getTime() <= nextWeek;
                }).length} detail="More reasons to get out" color="lavender" />
                <StatCard icon="users" label="Clubs to explore" value={clubs.length} detail="Your next thing is here" color="peach" />
                <StatCard icon="spark" label="Campus community" value={memberCount.toLocaleString()} detail="And growing every day" color="mint" />
                <StatCard icon="check" label="Your upcoming plans" value={registrations.length} detail="You’re showing up!" color="sky" />
              </section>

              <section className="section-block">
                <div className="section-heading"><div><div className="eyebrow">FIND YOUR NEXT THING</div><h2>Events worth showing up for</h2></div><button className="text-button" onClick={() => setView('my-events')}>My event list <Icon name="arrow" size={15} /></button></div>
                <div className="filter-row">
                  {categories.map((filter) => <button key={filter} className={`filter-chip ${category === filter ? 'filter-selected' : ''}`} onClick={() => setCategory(filter)}>{filter}</button>)}
                </div>
                <EventGrid events={visibleEvents.slice(0, 3)} registrations={registeredIds} onRegister={toggleRegistration} busy={busy} />
              </section>

              <div className="lower-grid">
                <section className="section-block compact-section"><div className="section-heading"><div><div className="eyebrow">BE PART OF SOMETHING</div><h2>Clubs you might love</h2></div><button className="text-button" onClick={() => setView('clubs')}>All clubs <Icon name="arrow" size={15} /></button></div><ClubGrid clubs={clubs.slice(0, 3)} onJoin={joinClub} busy={busy} /></section>
                <AnnouncementPanel announcements={announcements.slice(0, 3)} onViewAll={() => setView('announcements')} />
              </div>
            </>
          )}

          {view === 'my-events' && <section className="section-block view-section"><div className="eyebrow">YOUR CALENDAR, SORTED</div><div className="section-heading"><div><h1>My events</h1><p className="section-subtitle">All the good things you’ve got coming up.</p></div></div><EventGrid events={visibleEvents} registrations={registeredIds} onRegister={toggleRegistration} busy={busy} /><ParticipationHistory registrations={registrations.filter((item) => new Date(item.startsAt) < new Date())} /></section>}
          {view === 'manage' && <OrganizerDashboard events={events.filter((event) => user.role === 'ADMIN' || event.organizerId === user.id)} token={token} onEdit={(event) => { setEditingEvent(event); setModal(true); }} onDelete={deleteEvent} onNotice={setNotice} />}
          {view === 'clubs' && <section className="section-block view-section"><div className="eyebrow">FIND YOUR PEOPLE</div><div className="section-heading"><div><h1>Clubs & communities</h1><p className="section-subtitle">There’s a place for whatever makes you, you.</p></div>{canOrganize && <button className="button button-dark" onClick={() => setClubModal(true)}><Icon name="plus" size={15} /> Create club</button>}</div><ClubGrid clubs={clubs} onJoin={joinClub} busy={busy} /></section>}
          {view === 'announcements' && <section className="section-block view-section"><div className="eyebrow">THE LATEST FROM CAMPUS</div><div className="section-heading"><div><h1>Notice board</h1><p className="section-subtitle">A little catch-up on what’s happening around you.</p></div>{canOrganize && <button className="button button-dark" onClick={() => setAnnouncementModal(true)}><Icon name="plus" size={15} /> Post update</button>}</div><div className="announcement-list">{announcements.map((item) => <AnnouncementCard key={item.id} item={item} />)}</div></section>}
          {view === 'admin' && user.role === 'ADMIN' && <AdminDashboard summary={adminSummary} users={adminUsers} onRoleChange={updateRole} />}

          <footer className="footer"><span>Made for the moments that make campus.</span><span>CampusConnect <span className="footer-separator">·</span> Northstar University</span></footer>
        </div>
      </main>
      {mobileNav && <button className="mobile-backdrop" onClick={() => setMobileNav(false)} aria-label="Close navigation" />}
      {modal && <CreateEventModal clubs={clubs} event={editingEvent} onClose={() => { setModal(false); setEditingEvent(null); }} onSubmit={createEvent} busy={busy} />}
      {announcementModal && <CreateAnnouncementModal clubs={clubs} onClose={() => setAnnouncementModal(false)} onSubmit={createAnnouncement} busy={busy} />}
      {clubModal && <CreateClubModal onClose={() => setClubModal(false)} onSubmit={createClub} busy={busy} />}
      {aiOpen && <AiAssistantPanel
        messages={aiMessages}
        input={aiInput}
        setInput={setAiInput}
        busy={aiBusy}
        onClose={() => setAiOpen(false)}
        onAsk={handleAiAsk}
        suggestedPrompts={['Best events this week', 'What clubs should I join?', 'What’s happening on campus?']}
      />}
      {notice && <div className="toast"><span className="toast-check"><Icon name="check" size={14} /></span>{notice}<button onClick={() => setNotice('')} aria-label="Dismiss"><Icon name="close" size={15} /></button></div>}
    </div>
  );
}

function AiAssistantPanel({ messages, input, setInput, busy, onClose, onAsk, suggestedPrompts }) {
  return <div className="ai-panel">
    <div className="ai-header">
      <div>
        <div className="eyebrow"><span className="eyebrow-dot" /> Campus AI</div>
        <strong>Smart campus guide</strong>
      </div>
      <button className="icon-button" onClick={onClose} aria-label="Close AI Assistant"><Icon name="close" size={15} /></button>
    </div>
    <div className="ai-suggestions">
      {suggestedPrompts.map((prompt) => (
        <button key={prompt} className="ai-chip" onClick={() => onAsk(prompt)}>{prompt}</button>
      ))}
    </div>
    <div className="ai-messages">
      {messages.map((message, index) => (
        <div key={`${message.role}-${index}`} className={`ai-message ${message.role}`}>
          {message.text}
        </div>
      ))}
      {busy && <div className="ai-message assistant">Thinking through the best options for you…</div>}
    </div>
    <form className="ai-form" onSubmit={(event) => { event.preventDefault(); onAsk(); }}>
      <input value={input} onChange={(event) => setInput(event.target.value)} placeholder="Ask about events, clubs, or campus news" aria-label="Ask Campus AI" />
      <button className="button button-dark" type="submit" disabled={busy || !input.trim()}>
        {busy ? 'Thinking…' : 'Ask'}
      </button>
    </form>
  </div>;
}

function StatCard({ icon, label, value, detail, color }) {
  return <div className="stat-card"><span className={`stat-icon ${color}`}><Icon name={icon} size={18} /></span><div className="stat-copy"><span>{label}</span><strong>{value}</strong><small>{detail}</small></div></div>;
}

function EventGrid({ events, registrations, onRegister, busy }) {
  if (events.length === 0) return <div className="empty-state"><span className="empty-icon"><Icon name="calendar" size={22} /></span><strong>Nothing on the calendar just yet</strong><p>Try another category or check back soon — campus is always coming up with something.</p></div>;
  return <div className="event-grid">{events.map((event) => {
    const going = registrations.has(event.id);
    return <article className="event-card" key={event.id}>
      <div className="event-image" style={{ backgroundImage: `url("${event.imageUrl || fallbackImages[event.category] || fallbackImages.Social}")` }}><span className={`category-tag ${event.category?.toLowerCase()}`}>{event.category}</span><button className={`save-event ${going ? 'saved' : ''}`} title={going ? 'Leave event' : 'Register for event'} onClick={() => onRegister(event)} disabled={busy}><Icon name={going ? 'check' : 'plus'} size={16} /></button></div>
      <div className="event-info"><div className="event-date"><span>{formatDay(event.startsAt)}</span><span>·</span><span>{formatTime(event.startsAt)}</span></div><h3>{event.title}</h3><div className="event-club">{event.clubName || 'Campus community'}</div><div className="event-card-bottom"><span className="event-place"><Icon name="pin" size={14} />{event.venue}</span><button className={`register-link ${going ? 'registered' : ''}`} onClick={() => onRegister(event)} disabled={busy}>{going ? 'Going ✓' : 'Join us'} <Icon name="arrow" size={14} /></button></div></div>
    </article>;
  })}</div>;
}

function ClubGrid({ clubs, onJoin, busy }) {
  if (clubs.length === 0) return <div className="empty-state compact-empty"><strong>No clubs yet</strong><p>Check back soon for student communities.</p></div>;
  const colors = ['club-lilac', 'club-coral', 'club-blue', 'club-green', 'club-yellow', 'club-pink'];
  return <div className="club-grid">{clubs.map((club, index) => <article className="club-card" key={club.id}><div className={`club-art ${colors[index % colors.length]}`}><span>{club.shortName || club.name.slice(0, 2).toUpperCase()}</span><div className="club-star">✳</div></div><div className="club-card-copy"><div className="club-members"><Icon name="users" size={14} /> {club.memberCount} members</div><h3>{club.name}</h3><p>{club.description}</p><button className="club-join" onClick={() => onJoin(club)} disabled={busy || club.joined}>{club.joined ? 'Member ✓' : 'Meet the club'} {!club.joined && <Icon name="arrow" size={14} />}</button></div></article>)}</div>;
}

function AnnouncementPanel({ announcements, onViewAll }) {
  return <section className="announcements-panel"><div className="section-heading"><div><div className="eyebrow">GOOD TO KNOW</div><h2>Campus notes</h2></div><button className="icon-button" onClick={onViewAll} aria-label="View all announcements"><Icon name="arrow" size={16} /></button></div><div className="notes-list">{announcements.length === 0 ? <p className="muted-copy">Nothing new right now. Enjoy the quiet.</p> : announcements.map((item) => <AnnouncementCard item={item} key={item.id} compact />)}</div><button className="all-notes" onClick={onViewAll}>All announcements <Icon name="arrow" size={14} /></button></section>;
}

function AnnouncementCard({ item, compact = false }) {
  return <article className={`announcement-card ${compact ? 'note-compact' : ''}`}><span className="announcement-dot" /><div className="announcement-copy"><div className="announcement-meta">{item.clubName || 'Campus life'} <span>·</span> {new Intl.DateTimeFormat('en', { month: 'short', day: 'numeric' }).format(new Date(item.createdAt))}</div><h3>{item.title}</h3><p>{item.body}</p></div></article>;
}

function toLocalInput(date) {
  if (!date) return '';
  const parsed = new Date(date);
  const offset = parsed.getTimezoneOffset() * 60000;
  return new Date(parsed.getTime() - offset).toISOString().slice(0, 16);
}

function CreateEventModal({ clubs, event, onClose, onSubmit, busy }) {
  const [form, setForm] = useState(() => event ? {
    title: event.title,
    category: event.category,
    description: event.description,
    venue: event.venue,
    startsAt: toLocalInput(event.startsAt),
    endsAt: toLocalInput(event.endsAt),
    capacity: String(event.capacity),
    imageUrl: event.imageUrl || '',
    clubId: event.clubId || '',
  } : { title: '', category: 'Workshop', description: '', venue: '', startsAt: '', endsAt: '', capacity: '60', imageUrl: '', clubId: clubs[0]?.id || '' });
  const update = (key) => (event) => setForm({ ...form, [key]: event.target.value });
  function submit(event) {
    event.preventDefault();
    onSubmit({ ...form, capacity: Number(form.capacity), clubId: form.clubId ? Number(form.clubId) : null });
  }
  return <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}><form className="event-modal" onSubmit={submit}><div className="modal-header"><div><div className="eyebrow">MAKE IT HAPPEN</div><h2>{event ? 'Edit your event' : 'Create an event'}</h2></div><button type="button" className="icon-button" onClick={onClose} aria-label="Close"><Icon name="close" /></button></div><label>Event name<input required value={form.title} onChange={update('title')} placeholder="e.g. Build your first app" /></label><div className="form-two-col"><label>Category<select value={form.category} onChange={update('category')}>{categories.slice(1).map((item) => <option key={item}>{item}</option>)}</select></label><label>Club<select value={form.clubId} onChange={update('clubId')}><option value="">Campus community</option>{clubs.map((club) => <option key={club.id} value={club.id}>{club.name}</option>)}</select></label></div><label>What’s it about?<textarea required rows="3" value={form.description} onChange={update('description')} placeholder="Tell people why they should come along..." /></label><label>Where<input required value={form.venue} onChange={update('venue')} placeholder="Building, room or online" /></label><div className="form-two-col"><label>Starts<input required type="datetime-local" value={form.startsAt} onChange={update('startsAt')} /></label><label>Ends<input required type="datetime-local" value={form.endsAt} onChange={update('endsAt')} /></label></div><div className="form-two-col"><label>Spots available<input required type="number" min="1" max="2000" value={form.capacity} onChange={update('capacity')} /></label><label>Cover image URL<input type="url" value={form.imageUrl} onChange={update('imageUrl')} placeholder="https://…" /></label></div><div className="modal-actions"><button type="button" className="button button-outline" onClick={onClose}>Cancel</button><button type="submit" className="button button-dark" disabled={busy}>{busy ? 'Saving…' : event ? 'Save changes' : 'Publish event'} <Icon name="arrow" size={15} /></button></div></form></div>;
}

function OrganizerDashboard({ events, token, onEdit, onDelete, onNotice }) {
  const [selectedId, setSelectedId] = useState(null);
  const [attendees, setAttendees] = useState([]);
  const [loading, setLoading] = useState(false);

  async function showAttendees(eventId) {
    setSelectedId(eventId);
    setLoading(true);
    try {
      setAttendees(await request(`/events/${eventId}/attendees`, { token }));
    } catch (issue) {
      onNotice(issue.message);
    } finally {
      setLoading(false);
    }
  }

  async function markPresent(userId) {
    try {
      await request(`/events/${selectedId}/attendance/${userId}`, { token, method: 'POST' });
      setAttendees(await request(`/events/${selectedId}/attendees`, { token }));
      onNotice('Attendance saved.');
    } catch (issue) {
      onNotice(issue.message);
    }
  }

  const selectedEvent = events.find((event) => event.id === selectedId);
  return <section className="section-block view-section"><div className="eyebrow">YOUR EVENTS, ALL IN ONE PLACE</div><div className="section-heading"><div><h1>Event manager</h1><p className="section-subtitle">Update your event details, manage sign-ups, and take attendance.</p></div></div>{events.length === 0 ? <div className="empty-state"><span className="empty-icon"><Icon name="calendar" size={22} /></span><strong>No events to manage yet</strong><p>Create your first campus event to get things started.</p></div> : <div className="management-list">{events.map((event) => <article className="management-event" key={event.id}><div className="management-event-copy"><span className="category-tag">{event.category}</span><h3>{event.title}</h3><span>{formatDay(event.startsAt)} · {formatTime(event.startsAt)} · {event.venue}</span><small>{event.registeredCount} / {event.capacity} spots claimed</small></div><div className="management-actions"><button className="button button-outline" onClick={() => showAttendees(event.id)}>Attendance list</button><button className="button button-outline" onClick={() => onEdit(event)}>Edit</button><button className="icon-button" onClick={() => onDelete(event)} aria-label={`Delete ${event.title}`}><Icon name="close" size={15} /></button></div></article>)}</div>}{selectedEvent && <div className="attendee-panel"><div className="section-heading"><div><div className="eyebrow">EVENT ROSTER</div><h2>{selectedEvent.title}</h2></div><button className="icon-button" onClick={() => setSelectedId(null)} aria-label="Close roster"><Icon name="close" size={16} /></button></div>{loading ? <p className="section-subtitle">Loading registrations…</p> : attendees.length === 0 ? <p className="section-subtitle">No one has registered yet.</p> : <div className="attendee-list">{attendees.map((attendee) => <div className="attendee-row" key={attendee.id}><div className="profile-avatar">{attendee.name.split(' ').map((part) => part[0]).slice(0, 2).join('')}</div><div className="attendee-copy"><strong>{attendee.name}</strong><small>{attendee.email}</small></div><button className={`attendance-button ${attendee.attended ? 'attendance-done' : ''}`} onClick={() => markPresent(attendee.id)} disabled={attendee.attended}>{attendee.attended ? 'Present ✓' : 'Mark present'}</button></div>)}</div>}</div>}</section>;
}

function AdminDashboard({ summary, users, onRoleChange }) {
  if (!summary) return <section className="section-block view-section"><div className="eyebrow">CAMPUS AT A GLANCE</div><div className="section-heading"><div><h1>Admin panel</h1><p className="section-subtitle">Loading your campus overview…</p></div></div></section>;
  const metrics = [['Student accounts', summary.users], ['Published events', summary.events], ['Active clubs', summary.clubs], ['Event sign-ups', summary.registrations], ['Attendances marked', summary.attended]];
  return <section className="section-block view-section"><div className="eyebrow">CAMPUS AT A GLANCE</div><div className="section-heading"><div><h1>Admin panel</h1><p className="section-subtitle">The whole campus community, in one view.</p></div></div><div className="admin-metrics">{metrics.map(([label, value]) => <div className="admin-metric" key={label}><span>{label}</span><strong>{value}</strong></div>)}</div><div className="admin-user-panel"><div className="section-heading"><div><div className="eyebrow">PEOPLE & PERMISSIONS</div><h2>Campus members</h2></div></div><div className="admin-user-list">{users.map((member) => <div className="admin-user-row" key={member.id}><div className="profile-avatar">{member.name.split(' ').map((part) => part[0]).slice(0, 2).join('')}</div><div className="attendee-copy"><strong>{member.name}</strong><small>{member.email}</small></div><select aria-label={`Role for ${member.name}`} value={member.role} onChange={(event) => onRoleChange(member.id, event.target.value)}><option value="STUDENT">Student</option><option value="ORGANIZER">Organizer</option><option value="ADMIN">Admin</option></select></div>)}</div></div></section>;
}

function ParticipationHistory({ registrations }) {
  return <section className="history-section"><div className="section-heading"><div><div className="eyebrow">YOUR CAMPUS STORY</div><h2>Participation history</h2></div></div>{registrations.length === 0 ? <p className="section-subtitle">Your past events will show up here after you’ve been.</p> : <div className="history-list">{registrations.map((item) => <div className="history-row" key={item.eventId}><span className="history-check"><Icon name={item.attended ? 'check' : 'calendar'} size={15} /></span><div className="attendee-copy"><strong>{item.title}</strong><small>{formatDay(item.startsAt)}</small></div><span className={`history-status ${item.attended ? 'history-attended' : ''}`}>{item.attended ? 'Attended' : 'Registered'}</span></div>)}</div>}</section>;
}

function CreateAnnouncementModal({ clubs, onClose, onSubmit, busy }) {
  const [form, setForm] = useState({ title: '', body: '', clubId: '' });
  const update = (key) => (event) => setForm({ ...form, [key]: event.target.value });
  return <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}><form className="event-modal" onSubmit={(event) => { event.preventDefault(); onSubmit({ ...form, clubId: form.clubId ? Number(form.clubId) : null }); }}><div className="modal-header"><div><div className="eyebrow">KEEP EVERYONE IN THE LOOP</div><h2>Post an announcement</h2></div><button type="button" className="icon-button" onClick={onClose} aria-label="Close"><Icon name="close" /></button></div><label>Headline<input required maxLength="160" value={form.title} onChange={update('title')} placeholder="What should everyone know?" /></label><label>Message<textarea required rows="5" maxLength="1200" value={form.body} onChange={update('body')} placeholder="Share the details with your campus community..." /></label><label>Post on behalf of<select value={form.clubId} onChange={update('clubId')}><option value="">Campus community</option>{clubs.map((club) => <option key={club.id} value={club.id}>{club.name}</option>)}</select></label><div className="modal-actions"><button type="button" className="button button-outline" onClick={onClose}>Cancel</button><button type="submit" className="button button-dark" disabled={busy}>{busy ? 'Posting…' : 'Post update'} <Icon name="arrow" size={15} /></button></div></form></div>;
}

function CreateClubModal({ onClose, onSubmit, busy }) {
  const [form, setForm] = useState({ name: '', shortName: '', description: '' });
  const update = (key) => (event) => setForm({ ...form, [key]: event.target.value });
  return <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}><form className="event-modal" onSubmit={(event) => { event.preventDefault(); onSubmit(form); }}><div className="modal-header"><div><div className="eyebrow">START SOMETHING TOGETHER</div><h2>Create a club</h2></div><button type="button" className="icon-button" onClick={onClose} aria-label="Close"><Icon name="close" /></button></div><label>Club name<input required maxLength="120" value={form.name} onChange={update('name')} placeholder="e.g. Film Photography Club" /></label><label>Short name<input required maxLength="8" value={form.shortName} onChange={update('shortName')} placeholder="FPC" /></label><label>What brings your club together?<textarea required rows="4" maxLength="500" value={form.description} onChange={update('description')} placeholder="Describe the club and who might feel at home there..." /></label><div className="modal-actions"><button type="button" className="button button-outline" onClick={onClose}>Cancel</button><button type="submit" className="button button-dark" disabled={busy}>{busy ? 'Creating…' : 'Create club'} <Icon name="arrow" size={15} /></button></div></form></div>;
}

function LoginScreen({ onSubmit, error, busy }) {
  const [createAccount, setCreateAccount] = useState(false);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('student@northstar.edu');
  const [password, setPassword] = useState('Campus123!');
  function submit(event) { event.preventDefault(); onSubmit(email, password, createAccount, name); }
  return <div className="login-page"><div className="login-visual"><div className="login-brand brand"><span className="brand-mark"><Icon name="spark" size={20} /></span><span>campus<span className="brand-light">connect</span></span></div><div className="visual-orbit orbit-one" /><div className="visual-orbit orbit-two" /><div className="login-visual-copy"><div className="hero-kicker"><span className="live-dot" /> THE CAMPUS IS YOURS</div><h1>Find your<br />kind of <span>people.</span></h1><p>Good events. Great people. A campus that feels a little more like yours.</p><div className="login-decoration"><div className="decoration-card deco-one"><span className="deco-emoji">🎨</span><span>Art society</span></div><div className="decoration-card deco-two"><span className="deco-emoji">✦</span><span>Friday, 4 pm</span></div><div className="decoration-card deco-three"><span className="deco-emoji">☕</span><span>New friends</span></div></div></div><div className="login-visual-footer">NORTHSTAR UNIVERSITY <span>·</span> EST. 1987</div></div><div className="login-form-side"><div className="login-form-wrap"><div className="eyebrow"><span className="eyebrow-dot" /> YOUR CAMPUS, CONNECTED</div><h2>{createAccount ? 'Join your community.' : 'Hey, welcome back.'}</h2><p className="login-subtitle">{createAccount ? 'A world of campus life is waiting for you.' : 'Your next favorite campus moment is waiting.'}</p><form onSubmit={submit} className="login-form">{createAccount && <label>Your name<input value={name} onChange={(event) => setName(event.target.value)} autoComplete="name" required placeholder="Alex Morgan" /></label>}<label>University email<input type="email" value={email} onChange={(event) => setEmail(event.target.value)} autoComplete="email" required placeholder="you@university.edu" /></label><label>Password<input type="password" value={password} onChange={(event) => setPassword(event.target.value)} autoComplete={createAccount ? 'new-password' : 'current-password'} required minLength="8" /></label>{error && <div className="login-error">{error}</div>}<button className="button button-dark login-submit" disabled={busy}>{busy ? 'One moment…' : createAccount ? 'Create your account' : 'Sign in'} <Icon name="arrow" size={16} /></button></form><div className="account-toggle">{createAccount ? 'Already part of the campus?' : 'New around here?'} <button onClick={() => setCreateAccount(!createAccount)}>{createAccount ? 'Sign in' : 'Create an account'}</button></div><div className="demo-access"><span className="demo-icon"><Icon name="shield" size={16} /></span><div><strong>Want to see the organizer view?</strong><p>Use <code>organizer@northstar.edu</code> · <code>Campus123!</code></p><button onClick={() => { setEmail('organizer@northstar.edu'); setPassword('Campus123!'); }}>Fill demo organizer login <Icon name="arrow" size={13} /></button></div></div><div className="login-legal">By continuing, you agree to our community guidelines.<br />Built for students, by students <span>♥</span></div></div></div></div>;
}

export default App;
