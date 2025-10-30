import React from 'react';
import './CSS/AboutPage.css';


const AboutPage = ({ onClose }) => {
    return (
        <div className="about-page-overlay">
            <div className="about-page">
                <div className="about-page-header">
                    <h2>О нас</h2>
                </div>

                <div className="about-page-content">
                    <div className="about-section">
                        <div className="about-icon">🎓</div>
                        <h3>Study Service</h3>
                        <p className="about-subtitle">
                            Ваш надежный помощник в учебном процессе
                        </p>
                    </div>

                    <div className="mission-section">
                        <h4>Наша миссия</h4>
                        <p>
                            Мы создаем удобную и безопасную платформу для студентов,
                            где каждый может получить помощь в учебе или заработать
                            на своих знаниях, помогая другим.
                        </p>
                    </div>

                    <div className="features-grid">
                        <div className="feature-item">
                            <div className="feature-icon">⚡</div>
                            <div className="feature-content">
                                <h5>Быстро</h5>
                                <p>Находите исполнителей или заказы за несколько минут</p>
                            </div>
                        </div>

                        <div className="feature-item">
                            <div className="feature-icon">🛡️</div>
                            <div className="feature-content">
                                <h5>Безопасно</h5>
                                <p>Все платежи защищены, работа проверяется перед оплатой</p>
                            </div>
                        </div>

                        <div className="feature-item">
                            <div className="feature-icon">💎</div>
                            <div className="feature-content">
                                <h5>Качественно</h5>
                                <p>Только проверенные исполнители с опытом работы</p>
                            </div>
                        </div>

                        <div className="feature-item">
                            <div className="feature-icon">🔒</div>
                            <div className="feature-content">
                                <h5>Конфиденциально</h5>
                                <p>Ваши данные и заказы защищены приватностью</p>
                            </div>
                        </div>
                    </div>

                    <div className="stats-section">
                        <h4>Мы в цифрах</h4>
                        <div className="stats-grid">
                            <div className="stat-card">
                                <div className="stat-number">1000+</div>
                                <div className="stat-label">студентов</div>
                            </div>
                            <div className="stat-card">
                                <div className="stat-number">500+</div>
                                <div className="stat-label">исполнителей</div>
                            </div>
                            <div className="stat-card">
                                <div className="stat-number">95%</div>
                                <div className="stat-label">успешных сделок</div>
                            </div>
                            <div className="stat-card">
                                <div className="stat-number">24/7</div>
                                <div className="stat-label">поддержка</div>
                            </div>
                        </div>
                    </div>

                    <div className="how-it-works">
                        <h4>Как работает платформа?</h4>
                        <div className="steps">
                            <div className="step">
                                <span className="step-number">1</span>
                                <div className="step-content">
                                    <h5>Выбор роли</h5>
                                    <p>Определитесь - вы Студент (ищете помощь) или Исполнитель (предлагаете услуги)</p>
                                </div>
                            </div>
                            <div className="step">
                                <span className="step-number">2</span>
                                <div className="step-content">
                                    <h5>Создание профиля</h5>
                                    <p>Заполните информацию о себе для лучшего взаимодействия</p>
                                </div>
                            </div>
                            <div className="step">
                                <span className="step-number">3</span>
                                <div className="step-content">
                                    <h5>Поиск и взаимодействие</h5>
                                    <p>Найдите подходящего исполнителя или интересный заказ</p>
                                </div>
                            </div>
                            <div className="step">
                                <span className="step-number">4</span>
                                <div className="step-content">
                                    <h5>Выполнение работы</h5>
                                    <p>Обсудите детали, договоритесь о сроках и приступайте к работе</p>
                                </div>
                            </div>
                            <div className="step">
                                <span className="step-number">5</span>
                                <div className="step-content">
                                    <h5>Завершение сделки</h5>
                                    <p>Получите готовую работу и произведите оплату</p>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="values-section">
                        <h4>Наши ценности</h4>
                        <div className="values-list">
                            <div className="value-item">
                                <strong>Качество:</strong> Мы стремимся к высокому уровню выполняемых работ
                            </div>
                            <div className="value-item">
                                <strong>Надежность:</strong> Гарантируем безопасность и конфиденциальность
                            </div>
                            <div className="value-item">
                                <strong>Поддержка:</strong> Всегда готовы помочь в сложных ситуациях
                            </div>
                            <div className="value-item">
                                <strong>Развитие:</strong> Постоянно улучшаем платформу для вашего комфорта
                            </div>
                        </div>
                    </div>

                    <div className="contact-section">
                        <h4>Свяжитесь с нами</h4>
                        <p>
                            Есть вопросы или предложения? Мы всегда рады помочь!
                        </p>
                        <div className="contact-info">
                            <div className="contact-item">
                                <span className="contact-label">Поддержка:</span>
                                <span className="contact-value">@studyservice_support</span>
                            </div>
                            <div className="contact-item">
                                <span className="contact-label">Email:</span>
                                <span className="contact-value">support@studyservice.ru</span>
                            </div>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    );
};

export default AboutPage;