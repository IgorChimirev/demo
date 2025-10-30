import React, { useState, useEffect, lazy, Suspense } from 'react';
import { Routes, Route, useNavigate } from 'react-router-dom';
import HamburgerMenu from './components/HamburgerMenu';
import UserAvatar from './components/UserAvatar';
import OfferModal from './components/OfferModal';
import './App.css';


const ExecutorForm = lazy(() => import('./components/ExecutorForm'));
const ExecutorDashboard = lazy(() => import('./components/ExecutorDashboard'));
const ClientDashboard = lazy(() => import('./components/ClientDashboard'));
const AboutPage = lazy(() => import('./components/AboutPage'));


const LoadingSpinner = () => (
    <div className="loading-spinner">
        <div className="spinner"></div>
        <p>Загрузка...</p>
    </div>
);

function App() {
    const [isMenuActive, setIsMenuActive] = useState(false);
    const [user, setUser] = useState(null);
    const [tg, setTg] = useState(null);
    const [hasExecutorProfile, setHasExecutorProfile] = useState(false);
    const [showOffer, setShowOffer] = useState(false);
    const [acceptedOffer, setAcceptedOffer] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const navigate = useNavigate();


    const initializeUser = React.useCallback(async (userData) => {
        try {
            await fetch('/api/users', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    id: userData.id,
                    username: userData.username
                })
            });

            const response = await fetch(`/api/users/${userData.id}/accepted-offer`);
            if (response.ok) {
                const hasAccepted = await response.json();
                setAcceptedOffer(hasAccepted);
                if (!hasAccepted) {
                    setShowOffer(true);
                }
            }

            if (userData?.id) {
                checkExecutorProfile(userData.id);
            }
        } catch (error) {
            console.error('Ошибка при инициализации пользователя:', error);
        } finally {
            setIsLoading(false);
        }
    }, []);

    const checkExecutorProfile = React.useCallback(async (userId) => {
        if (!userId) return;

        try {
            const response = await fetch(`/api/executors/user/${userId}`);
            if (response.ok) {
                const data = await response.json();
                const hasProfile = data.length > 0;
                setHasExecutorProfile(hasProfile);
                return hasProfile;
            }
        } catch (error) {
            console.error('Ошибка при проверке профиля исполнителя:', error);
            return false;
        }
    }, []);

    const handleAcceptOffer = React.useCallback(async () => {
        try {
            const response = await fetch(`/api/users/${user.id}/accept-offer`, {
                method: 'POST'
            });

            if (response.ok) {
                setAcceptedOffer(true);
                setShowOffer(false);
            }
        } catch (error) {
            console.error('Ошибка при принятии оферты:', error);
        }
    }, [user?.id]);

    const handleClose = React.useCallback(() => {
        if (tg) {
            if (isMenuActive) {
                tg.BackButton.show();
            } else {
                tg.BackButton.hide();
            }
        }
    }, [tg, isMenuActive]);

    const handleExecutorClick = React.useCallback(async (e) => {
        e.preventDefault();

        if (!acceptedOffer) {
            setShowOffer(true);
            return;
        }

        if (user?.id) {
            const hasProfile = await checkExecutorProfile(user.id);

            if (hasProfile) {
                console.log('Открываем дашборд исполнителя');
                handleNavigate(e, "/executerdashbord");
                if (tg) tg.BackButton.show();
            } else {
                console.log('Открываем форму исполнителя');
                handleNavigate(e, "/executerform");
                if (tg) tg.BackButton.show();
            }
        } else {
            console.log('Пользователь не авторизован');
        }
    }, [acceptedOffer, user, tg, checkExecutorProfile]);

    const toggleMenu = React.useCallback(() => {
        const newState = !isMenuActive;
        setIsMenuActive(newState);

        if (tg) {
            if (newState) {
                tg.BackButton.show();
            } else {
                tg.BackButton.hide();
            }
        }
    }, [isMenuActive, tg]);

    const handleNavigate = React.useCallback((e, path) => {
        if (!acceptedOffer && path !== '/') {
            e.preventDefault();
            setShowOffer(true);
            return;
        }

        e.preventDefault();
        setIsMenuActive(false);
        if (tg) {
            tg.BackButton.show();
        }
        navigate(path);
    }, [acceptedOffer, tg, navigate]);

    useEffect(() => {
        const telegramWebApp = window.Telegram?.WebApp;
        if (telegramWebApp) {
            telegramWebApp.ready();
            setTg(telegramWebApp);

            const userData = telegramWebApp.initDataUnsafe?.user;
            if (userData) {
                const userInfo = {
                    id: userData.id,
                    username: userData.username || `user_${userData.id}`,
                    first_name: userData.first_name,
                    last_name: userData.last_name,
                    photo_url: userData.photo_url
                };
                setUser(userInfo);
                localStorage.setItem('UserId', userData.id);
                initializeUser(userInfo);
            } else {
                setIsLoading(false);
            }

            telegramWebApp.BackButton.onClick(() => {
                if (window.location.pathname !== '/') {
                    navigate(-1);
                } else {
                    setIsMenuActive(false);
                    telegramWebApp.BackButton.hide();
                }
            });
        } else {
            setIsLoading(false);
        }
    }, [navigate, initializeUser]);

    if (isLoading) {
        return (
            <div className="loading">
                <div className="spinner"></div>
                <p>Загрузка...</p>
            </div>
        );
    }

    return (
        <div className="app">
            <OfferModal
                isOpen={showOffer}
                onAccept={handleAcceptOffer}
            />
            {!acceptedOffer && showOffer && <div className="interface-blocker"></div>}
            <div className="page-content">
                <Routes>
                    <Route path="/" element={
                        <>
                            <HamburgerMenu
                                isActive={isMenuActive}
                                onToggle={toggleMenu}
                                isHidden={isMenuActive}
                            />
                            <UserAvatar
                                user={user}
                                isHidden={isMenuActive}
                                onClick={() => console.log('Avatar clicked')}
                            />
                            <div className={`menu ${isMenuActive ? 'active' : ''}`}>
                                <ul>
                                    <li>
                                        <a href="#" onClick={handleExecutorClick}>
                                            {hasExecutorProfile ? 'Кабинет исполнителя' : 'Исполнитель'}
                                        </a>
                                    </li>
                                    <li>
                                        <a href="/clientdashbord" onClick={(e) => handleNavigate(e, '/clientdashbord')}>
                                            Покупатель
                                        </a>
                                    </li>
                                    <li>
                                        <a href="/aboutus" onClick={(e) => handleNavigate(e, '/aboutus')}>
                                            О нас
                                        </a>
                                    </li>
                                </ul>
                            </div>
                        </>
                    } />
                    <Route
                        path="/executerform"
                        element={
                            <Suspense fallback={<LoadingSpinner />}>
                                <ExecutorForm
                                    user={user}
                                    onSuccess={(e) => handleNavigate(e, '/executerdashbord')}
                                    onClose={handleClose}
                                />
                            </Suspense>
                        }
                    />
                    <Route
                        path="/executerdashbord"
                        element={
                            <Suspense fallback={<LoadingSpinner />}>
                                <ExecutorDashboard
                                    user={user}
                                    onClose={handleClose}
                                />
                            </Suspense>
                        }
                    />
                    <Route
                        path="/clientdashbord"
                        element={
                            <Suspense fallback={<LoadingSpinner />}>
                                <ClientDashboard
                                    user={user}
                                    onClose={handleClose}
                                />
                            </Suspense>
                        }
                    />
                    <Route
                        path="/aboutus"
                        element={
                            <Suspense fallback={<LoadingSpinner />}>
                                <AboutPage />
                            </Suspense>
                        }
                    />
                </Routes>
            </div>
        </div>
    );
}

export default App;