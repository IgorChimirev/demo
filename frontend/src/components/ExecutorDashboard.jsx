import React, { useState, useEffect } from 'react';
import './CSS/ExecutorDashboard.css';



const ExecutorDashboard = ({ onClose, user }) => {
    const [activeTab, setActiveTab] = useState('profile');
    const [executorData, setExecutorData] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isEditing, setIsEditing] = useState(false);
    useEffect(() => {
        fetchExecutorData();

    }, []);



    const fetchExecutorData = async () => {
        try {
            const response = await fetch(`/api/executors/user/${localStorage.getItem('UserId')}`);
            if (response.ok) {
                const data = await response.json();
                if (data.length > 0) {
                    setExecutorData(data[0]);
                }
            }
        } catch (error) {
            console.error('Ошибка при загрузке данных:', error);
        } finally {
            setIsLoading(false);
        }
    };

    const handleEditProfile = () => {
        setIsEditing(true);
    };

    const handleSaveProfile = async (updatedData) => {
        try {
            const response = await fetch(`/api/executors/${executorData.id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(updatedData),
            });

            if (response.ok) {
                const savedData = await response.json();
                setExecutorData(savedData);
                setIsEditing(false);
            } else {
                console.error('Ошибка при сохранении данных');
            }
        } catch (error) {
            console.error('Ошибка при сохранении:', error);
        }
    };

    const handleCancelEdit = () => {
        setIsEditing(false);
    };

    if (isLoading) {
        return (
            <div className="executor-dashboard-overlay">
                <div className="executor-dashboard">
                    <div className="loading">Загрузка...</div>
                </div>
            </div>
        );
    }

    if (!executorData) {
        return (
            <div className="executor-dashboard-overlay">
                <div className="executor-dashboard">
                    <div className="no-data">Данные не найдены</div>
                </div>
            </div>
        );
    }

    return (
        <div className="executor-dashboard-overlay">
            <div className="executor-dashboard">
                <div className="executor-dashboard-header">
                    <h2>Панель исполнителя</h2>
                </div>

                <div className="dashboard-tabs">
                    <button
                        className={`tab-button ${activeTab === 'profile' ? 'active' : ''}`}
                        onClick={() => setActiveTab('profile')}
                    >
                        Профиль
                    </button>
                    <button
                        className={`tab-button ${activeTab === 'responses' ? 'active' : ''}`}
                        onClick={() => setActiveTab('responses')}
                    >
                        Мои отклики
                    </button>
                    <button
                        className={`tab-button ${activeTab === 'completed' ? 'active' : ''}`}
                        onClick={() => setActiveTab('completed')}
                    >
                        Завершенные
                    </button>
                    <button
                        className={`tab-button ${activeTab === 'available' ? 'active' : ''}`}
                        onClick={() => setActiveTab('available')}
                    >
                        Доступные заказы
                    </button>
                </div>

                <div className="dashboard-content">
                    {activeTab === 'profile' && (
                        <ProfileTab
                            executorData={executorData}
                            onEdit={handleEditProfile}
                            isEditing={isEditing}
                            onSave={handleSaveProfile}
                            onCancel={handleCancelEdit}
                        />
                    )}
                    {activeTab === 'responses' && <ResponsesTab />}
                    {activeTab === 'completed' && <CompletedTab />}
                    {activeTab === 'available' && <AvailableOrdersTab executorData={executorData} />}
                </div>
            </div>
        </div>
    );
};


const ProfileTab = ({ executorData, onEdit, isEditing, onSave, onCancel }) => {
    const [formData, setFormData] = useState(executorData);

    useEffect(() => {
        setFormData(executorData);
    }, [executorData]);

    const handleInputChange = (field, value) => {
        setFormData(prev => ({
            ...prev,
            [field]: value
        }));
    };

    const handleSave = () => {
        onSave(formData);
    };

    const handleCancel = () => {
        setFormData(executorData);
        onCancel();
    };

    if (isEditing) {
        return (
            <div className="profile-tab">
                <div className="profile-header">
                    <h3>Редактирование профиля</h3>
                    <div className="edit-actions">
                        <button className="save-button" onClick={handleSave}>
                            Сохранить
                        </button>
                        <button className="cancel-button" onClick={handleCancel}>
                            Отмена
                        </button>
                    </div>
                </div>

                <div className="edit-form">
                    <div className="form-group">
                        <label>Имя и фамилия:</label>
                        <input
                            type="text"
                            value={formData.name || ''}
                            onChange={(e) => handleInputChange('name', e.target.value)}
                            className="form-input"
                        />
                    </div>

                    <div className="form-group">
                        <label>Категория услуг:</label>
                        <select
                            value={formData.category || ''}
                            onChange={(e) => handleInputChange('category', e.target.value)}
                            className="form-select"
                        >
                            <option value="">Выберите категорию</option>
                            <option value="development">Разработка</option>
                            <option value="design">Дизайн</option>
                            <option value="writing">Копирайтинг</option>
                            <option value="marketing">Маркетинг</option>
                            <option value="consulting">Консалтинг</option>
                        </select>
                    </div>

                    <div className="form-group">
                        <label>Опыт работы:</label>
                        <input
                            type="text"
                            value={formData.experience || ''}
                            onChange={(e) => handleInputChange('experience', e.target.value)}
                            className="form-input"
                            placeholder="Например: 3 года"
                        />
                    </div>

                    <div className="form-group">
                        <label>Стоимость услуг:</label>
                        <input
                            type="text"
                            value={formData.price || ''}
                            onChange={(e) => handleInputChange('price', e.target.value)}
                            className="form-input"
                            placeholder="Например: 1000 руб/час"
                        />
                    </div>

                    <div className="form-group">
                        <label>Контакты:</label>
                        <input
                            type="text"
                            value={formData.contacts || ''}
                            onChange={(e) => handleInputChange('contacts', e.target.value)}
                            className="form-input"
                            placeholder="Email, телефон или другие контакты"
                        />
                    </div>

                    <div className="form-group full-width">
                        <label>Описание услуг:</label>
                        <textarea
                            value={formData.description || ''}
                            onChange={(e) => handleInputChange('description', e.target.value)}
                            className="form-textarea"
                            placeholder="Подробно опишите ваши услуги и опыт..."
                            rows="4"
                        />
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="profile-tab">
            <div className="profile-header">
                <h3>Профиль исполнителя</h3>
                <button className="edit-button" onClick={onEdit}>
                    Редактировать
                </button>
            </div>

            <div className="profile-info">
                <div className="info-item">
                    <label>Имя и фамилия:</label>
                    <span>{executorData.name}</span>
                </div>
                <div className="info-item">
                    <label>Категория услуг:</label>
                    <span>{executorData.category}</span>
                </div>
                <div className="info-item">
                    <label>Опыт работы:</label>
                    <span>{executorData.experience}</span>
                </div>
                <div className="info-item">
                    <label>Стоимость услуг:</label>
                    <span>{executorData.price}</span>
                </div>
                <div className="info-item">
                    <label>Контакты:</label>
                    <span>{executorData.contacts}</span>
                </div>
                <div className="info-item full-width">
                    <label>Описание услуг:</label>
                    <p>{executorData.description}</p>
                </div>
            </div>

            <div className="profile-stats">
                <div className="stat-card">
                    <div className="stat-number">12</div>
                    <div className="stat-label">Выполнено заказов</div>
                </div>
                <div className="stat-card">
                    <div className="stat-number">4.8</div>
                    <div className="stat-label">Рейтинг</div>
                </div>
                <div className="stat-card">
                    <div className="stat-number">95%</div>
                    <div className="stat-label">Успешных работ</div>
                </div>
            </div>
        </div>
    );
};

const ResponsesTab = () => {
    const [responses, setResponses] = useState([]);


    const mockResponses = [
        { id: 1, order: 'Разработка лендинга', status: 'рассматривается', date: '2024-01-15', price: '15000 руб' },
        { id: 2, order: 'Дизайн мобильного приложения', status: 'одобрено', date: '2024-01-10', price: '25000 руб' },
        { id: 3, order: 'SEO оптимизация сайта', status: 'отклонено', date: '2024-01-05', price: '20000 руб' },
    ];

    useEffect(() => {
        setResponses(mockResponses);
    }, []);

    return (
        <div className="responses-tab">
            <h3>Мои отклики</h3>
            <div className="responses-list">
                {responses.map(response => (
                    <div key={response.id} className="response-card">
                        <div className="response-header">
                            <h4>{response.order}</h4>
                            <span className={`status ${response.status}`}>
                                {response.status}
                            </span>
                        </div>
                        <div className="response-details">
                            <div className="detail">
                                <label>Предложенная цена:</label>
                                <span>{response.price}</span>
                            </div>
                            <div className="detail">
                                <label>Дата отклика:</label>
                                <span>{response.date}</span>
                            </div>
                        </div>
                        <div className="response-actions">
                            <button className="action-button">Подробнее</button>
                            {response.status === 'рассматривается' && (
                                <button className="action-button cancel">Отозвать</button>
                            )}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};


const CompletedTab = () => {
    const [completedOrders, setCompletedOrders] = useState([]);

    const mockCompleted = [
        { id: 1, title: 'Разработка корпоративного сайта', client: 'ООО "ТехноПрофи"', date: '2024-01-12', rating: 5 },
        { id: 2, title: 'UI/UX дизайн приложения', client: 'Стартап "QuickPay"', date: '2024-01-08', rating: 4 },
        { id: 3, title: 'Техническая поддержка', client: 'ИП Иванов', date: '2024-01-03', rating: 5 },
    ];

    useEffect(() => {
        setCompletedOrders(mockCompleted);
    }, []);

    return (
        <div className="completed-tab">
            <h3>Завершенные заказы</h3>
            <div className="completed-list">
                {completedOrders.map(order => (
                    <div key={order.id} className="completed-card">
                        <div className="order-header">
                            <h4>{order.title}</h4>
                            <div className="rating">
                                {'★'.repeat(order.rating)}{'☆'.repeat(5 - order.rating)}
                            </div>
                        </div>
                        <div className="order-details">
                            <div className="detail">
                                <label>Клиент:</label>
                                <span>{order.client}</span>
                            </div>
                            <div className="detail">
                                <label>Дата завершения:</label>
                                <span>{order.date}</span>
                            </div>
                        </div>
                        <div className="order-actions">
                            <button className="action-button">Оставить отзыв</button>
                            <button className="action-button secondary">Повторить</button>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};


const AvailableOrdersTab = ({ executorData }) => {
    const [availableOrders, setAvailableOrders] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        fetchAvailableOrders();
    }, [executorData?.category]);

    const fetchAvailableOrders = async () => {
        try {
            setIsLoading(true);

            const response = await fetch(`/api/orders/active/category/${executorData?.category}/${localStorage.getItem('UserId')}`);
            if (response.ok) {
                const data = await response.json();
                setAvailableOrders(data);
            } else {
                console.error('Ошибка при загрузке доступных заказов');

                await loadAllActiveOrders();
            }
        } catch (error) {
            console.error('Ошибка при загрузке доступных заказов:', error);
            await loadAllActiveOrders();
        } finally {
            setIsLoading(false);
        }
    };

    const loadAllActiveOrders = async () => {
        try {
            const response = await fetch(`/api/orders/active`);
            if (response.ok) {
                const allOrders = await response.json();

                const filteredOrders = allOrders.filter(order =>
                    order.subject?.toLowerCase().includes(executorData?.category?.toLowerCase()) ||
                    order.description?.toLowerCase().includes(executorData?.category?.toLowerCase())
                );
                setAvailableOrders(filteredOrders);
            }
        } catch (error) {
            console.error('Ошибка при загрузке всех активных заказов:', error);
        }
    };

    const handleRespond = async (orderId) => {
        try {
            const response = await fetch(`/api/orders/${orderId}/accept?executorId=${localStorage.getItem('UserId')}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            if (response.ok) {
                alert("Отклик был отправлен");
            } else {
                alert('Ошибка при отправке отклика');
            }
        } catch (error) {
            console.error('Ошибка при отправке отклика:', error);
            alert('Ошибка при отправке отклика');
        }
    };

    if (isLoading) {
        return (
            <div className="available-tab">
                <h3>Доступные заказы</h3>
                <div className="loading">Загрузка заказов...</div>
            </div>
        );
    }

    return (
        <div className="available-tab">
            <div className="available-header">
                <h3>Доступные заказы</h3>
                <div className="category-filter">
                    <span>Категория: <strong>{executorData?.category || 'не указана'}</strong></span>
                </div>
            </div>

            {availableOrders.length === 0 ? (
                <div className="no-orders">
                    <p>Нет доступных заказов в вашей категории "{executorData?.category}"</p>
                </div>
            ) : (
                <div className="available-list">
                    {availableOrders.map(order => (
                        <div key={order.id} className="available-card">
                            <div className="order-header">
                                <h4>{order.subject} - {order.university}</h4>
                                <span className="category">{order.subject}</span>
                            </div>
                            <div className="order-details">
                                <div className="detail">
                                    <label>Бюджет:</label>
                                    <span>{order.price || 'Не указан'}</span>
                                </div>
                                <div className="detail">
                                    <label>Создан:</label>
                                    <span>{new Date(order.createdAt).toLocaleDateString('ru-RU')}</span>
                                </div>
                                <div className="detail">
                                    <label>ВУЗ:</label>
                                    <span>{order.university}</span>
                                </div>
                            </div>
                            <div className="order-description">
                                <p>{order.description}</p>
                            </div>
                            <div className="order-actions">
                                <button
                                    className="respond-button"
                                    onClick={() => handleRespond(order.id)}
                                >
                                    Откликнуться
                                </button>
                                <button className="action-button secondary">Подробнее</button>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default ExecutorDashboard;