// components/OfferModal.jsx
import React from 'react';
import '../../../../demo-my 5/frontend/src/components/CSS/OfferModal.css';

const OfferModal = ({ isOpen, onAccept }) => {
    if (!isOpen) return null;

    return (
        <div className="offer-modal-overlay">
            <div className="offer-modal">
                <div className="offer-header">
                    <h2>Договор оферты</h2>
                </div>

                <div className="offer-content">
                    <div className="offer-text">
                        <p>
                            Перед использованием приложения, пожалуйста, ознакомьтесь с нашими
                            условиями использования и политикой конфиденциальности.
                        </p>

                        <h3>Основные положения:</h3>
                        <ul>
                            <li>Вы соглашаетесь с обработкой ваших персональных данных</li>
                            <li>Принимаете условия использования сервиса</li>
                            <li>Соглашаетесь с правилами проведения сделок</li>
                            <li>Принимаете политику конфиденциальности</li>
                        </ul>

                        <p>
                            Полный текст договора оферты доступен по ссылке:
                            <a href="/offer-full" target="_blank"> Оферта</a>
                        </p>
                    </div>
                </div>

                <div className="offer-actions">
                    <button
                        className="accept-button"
                        onClick={onAccept}
                    >
                        Принять условия
                    </button>
                </div>
            </div>
        </div>
    );
};

export default OfferModal;