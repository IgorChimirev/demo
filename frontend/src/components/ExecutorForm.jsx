import React, { useState } from 'react';
import './CSS/ExecutorForm.css';

const ExecutorForm = ({ onClose,onSuccess, user }) => {
    const [formData, setFormData] = useState({
        name: '',
        category: '',
        description: '',
        price: '',
        experience: '',
        contacts: ''
    });

    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsSubmitting(true);

        try {
            const response = await fetch('/api/executors', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    ...formData,
                    telegramUserId: user?.id,
                    telegramUsername: user?.username
                }),
            });

            if (response.ok) {
                console.log('Данные успешно отправлены');
                if (onSuccess) {
                    onSuccess(e);
                } else {
                    onClose();
                }
            } else {
                console.error('Ошибка при отправке данных');
            }
        } catch (error) {
            console.error('Ошибка:', error);
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleChange = (e) => {
        setFormData({
            ...formData,
            [e.target.name]: e.target.value
        });
    };

    return (
        <div className="executor-form-overlay">
            <div className="executor-form">
                <div className="executor-form-header">
                    <h2>Форма исполнителя</h2>
                </div>

                <form onSubmit={handleSubmit} className="executor-form-content">
                    <div className="form-group">
                        <label htmlFor="name">Имя и фамилия</label>
                        <input
                            type="text"
                            id="name"
                            name="name"
                            value={formData.name}
                            onChange={handleChange}
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="category">Категория услуг</label>
                        <select
                            id="category"
                            name="category"
                            value={formData.category}
                            onChange={handleChange}
                            required
                        >
                            <option value="">Выберите категорию</option>
                            <option value="design">Дизайн</option>
                            <option value="development">Разработка</option>
                            <option value="marketing">Маркетинг</option>
                            <option value="writing">Копирайтинг</option>
                            <option value="consulting">Консультации</option>
                        </select>
                    </div>

                    <div className="form-group">
                        <label htmlFor="description">Описание услуг</label>
                        <textarea
                            id="description"
                            name="description"
                            value={formData.description}
                            onChange={handleChange}
                            rows="4"
                            placeholder="Опишите ваши услуги..."
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="price">Стоимость услуг</label>
                        <input
                            type="text"
                            id="price"
                            name="price"
                            value={formData.price}
                            onChange={handleChange}
                            placeholder="Например: 1000 руб/час"
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="experience">Опыт работы</label>
                        <input
                            type="text"
                            id="experience"
                            name="experience"
                            value={formData.experience}
                            onChange={handleChange}
                            placeholder="Например: 3 года"
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="contacts">Контакты</label>
                        <input
                            type="text"
                            id="contacts"
                            name="contacts"
                            value={formData.contacts}
                            onChange={handleChange}
                            placeholder="Telegram, email или телефон"
                            required
                        />
                    </div>

                    <div className="form-buttons">
                        <button
                            type="submit"
                            className="submit-button"
                            disabled={isSubmitting}
                        >
                            {isSubmitting ? 'Отправка...' : 'Отправить'}
                        </button>
                        <button type="button" className="cancel-button" onClick={onClose}>
                            Отмена
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default ExecutorForm;