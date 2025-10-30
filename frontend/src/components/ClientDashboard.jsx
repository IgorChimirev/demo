import React, { useState, useEffect } from 'react';
import './CSS/ClientDashboard.css';

const ClientDashboard = ({ onClose, user }) => {
    const [activeTab, setActiveTab] = useState('create');
    const [orders, setOrders] = useState([]);
    const [pastOrders, setPastOrders] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [currentUser, setCurrentUser] = useState(user);
    const userId = user?.id;

    const [orderForm, setOrderForm] = useState({
        university: '',
        subject: '',
        category: '',
        description: '',
        price: ''
    });

    useEffect(() => {
        const savedUser = localStorage.getItem('telegramUser');
        if (savedUser && !user) {
            setCurrentUser(JSON.parse(savedUser));
        } else if (user) {
            setCurrentUser(user);
            localStorage.setItem('telegramUser', JSON.stringify(user));
        }
    }, [user]);

    useEffect(() => {
        if (currentUser && currentUser.id) {
            loadOrders();
            loadPastOrders();
        }
    }, [currentUser]);

    const loadOrders = async () => {
        if (!currentUser?.id) return;

        setIsLoading(true);
        try {
            const response = await fetch(`/api/orders/user/${currentUser.id}/active`);
            if (response.ok) {
                const data = await response.json();
                setOrders(data);
            }
        } catch (error) {
            console.error('Ошибка при загрузке заказов:', error);
        } finally {
            setIsLoading(false);
        }
    };

    const loadPastOrders = async () => {
        if (!currentUser?.id) return;

        try {
            const response = await fetch(`/api/orders/user/${currentUser.id}/completed`);
            if (response.ok) {
                const data = await response.json();
                setPastOrders(data);
            }
        } catch (error) {
            console.error('Ошибка при загрузке прошлых заказов:', error);
        }
    };

    const handleInputChange = (field, value) => {
        setOrderForm(prev => ({
            ...prev,
            [field]: value
        }));
    };

    const handleCreateOrder = async (e) => {
        e.preventDefault();

        if (!orderForm.university || !orderForm.subject || !orderForm.category || !orderForm.description) {
            alert('Пожалуйста, заполните все обязательные поля');
            return;
        }

        setIsLoading(true);
        try {
            const orderData = {
                telegramUserId: currentUser.id,
                telegramUsername: currentUser.username || `${currentUser.first_name}${currentUser.last_name ? ' ' + currentUser.last_name : ''}`,
                university: orderForm.university,
                subject: orderForm.subject,
                category: orderForm.category,
                description: orderForm.description,
                price: orderForm.price || '0'
            };

            console.log('Sending order data:', orderData);

            const response = await fetch(`/api/orders/request`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(orderData)
            });

            if (response.ok) {
                const newOrder = await response.json();
                setOrders(prev => [newOrder, ...prev]);
                setOrderForm({
                    university: '',
                    subject: '',
                    category: '',
                    description: '',
                    price: ''
                });
                setActiveTab('current');
            } else {
                const errorText = await response.text();
                alert('Ошибка при создании заказа: ' + errorText);
            }
        } catch (error) {
            console.error('Network error:', error);
            alert('Ошибка при создании заказа: ' + error.message);
        } finally {
            setIsLoading(false);
        }
    };

    const handleCancelOrder = async (orderId) => {
        if (window.confirm('Вы уверены, что хотите отменить заказ?')) {
            try {
                const response = await fetch(`/api/orders/${orderId}`, {
                    method: 'DELETE'
                });

                if (response.ok) {
                    setOrders(prev => prev.filter(order => order.id !== orderId));
                } else {
                    alert('Ошибка при отмене заказа');
                }
            } catch (error) {
                console.error('Ошибка при отмене заказа:', error);
                alert('Ошибка при отмене заказа');
            }
        }
    };

    const formatDate = (dateString) => {
        if (!dateString) return '';
        const date = new Date(dateString);
        return date.toLocaleDateString('ru-RU');
    };

    const formatBudget = (price) => {
        if (!price || price === '0') return 'Не указана';
        return `${price} руб`;
    };

    return (
        <div className="client-dashboard-overlay">
            <div className="client-dashboard">
                <div className="client-dashboard-header">
                    <h2 >Панель покупателя</h2>
                </div>

                <div className="dashboard-tabs">
                    <button className={`tab-button ${activeTab === 'create' ? 'active' : ''}`} onClick={() => setActiveTab('create')}>
                        Создать заказ
                    </button>
                    <button className={`tab-button ${activeTab === 'current' ? 'active' : ''}`} onClick={() => setActiveTab('current')}>
                        Мои заказы
                    </button>
                    <button className={`tab-button ${activeTab === 'past' ? 'active' : ''}`} onClick={() => setActiveTab('past')}>
                        Прошлые заказы
                    </button>
                </div>

                <div className="dashboard-content">
                    {activeTab === 'create' && (
                        <CreateOrderTab
                            formData={orderForm}
                            onInputChange={handleInputChange}
                            onSubmit={handleCreateOrder}
                            isLoading={isLoading}
                        />
                    )}
                    {activeTab === 'current' && (
                        <CurrentOrdersTab
                            orders={orders}
                            onCancelOrder={handleCancelOrder}
                            isLoading={isLoading}
                            formatDate={formatDate}
                            formatBudget={formatBudget}
                        />
                    )}
                    {activeTab === 'past' && (
                        <PastOrdersTab
                            orders={pastOrders}
                            isLoading={isLoading}
                            formatDate={formatDate}
                            formatBudget={formatBudget}
                        />
                    )}
                </div>
            </div>
        </div>
    );
};

const CreateOrderTab = ({ formData, onInputChange, onSubmit, isLoading }) => {
    const universities = [
        'МГУ', 'ВШЭ', 'МФТИ', 'МГТУ', 'МИФИ', 'РЭУ', 'МАИ', 'Другой'
    ];

    const subjects = [
        'Математика', 'Физика', 'Химия', 'Информатика', 'Экономика',
        'Программирование', 'Дизайн', 'Маркетинг', 'Другое'
    ];


    const categories = [
        { value: 'development', label: 'Разработка' },
        { value: 'design', label: 'Дизайн' },
        { value: 'writing', label: 'Копирайтинг' },
        { value: 'marketing', label: 'Маркетинг' },
        { value: 'consulting', label: 'Консалтинг' }
    ];

    return (
        <div className="create-order-tab">
            <h3>Создать новый заказ</h3>

            <form onSubmit={onSubmit} className="order-form">
                <div className="form-group">
                    <label htmlFor="university">ВУЗ:</label>
                    <select
                        id="university"
                        value={formData.university}
                        onChange={(e) => onInputChange('university', e.target.value)}
                        className="form-select"
                        required
                    >
                        <option value="">Выберите ВУЗ</option>
                        {universities.map(uni => (
                            <option key={uni} value={uni}>{uni}</option>
                        ))}
                    </select>
                </div>

                <div className="form-group">
                    <label htmlFor="subject">Предмет:</label>
                    <select
                        id="subject"
                        value={formData.subject}
                        onChange={(e) => onInputChange('subject', e.target.value)}
                        className="form-select"
                        required
                    >
                        <option value="">Выберите предмет</option>
                        {subjects.map(subject => (
                            <option key={subject} value={subject}>{subject}</option>
                        ))}
                    </select>
                </div>

                {/* ТАК ЖЕ КАК У ИСПОЛНИТЕЛЯ */}
                <div className="form-group">
                    <label htmlFor="category">Категория:</label>
                    <select
                        id="category"
                        value={formData.category}
                        onChange={(e) => onInputChange('category', e.target.value)}
                        className="form-select"
                        required
                    >
                        <option value="">Выберите категорию</option>
                        {categories.map(category => (
                            <option key={category.value} value={category.value}>
                                {category.label}
                            </option>
                        ))}
                    </select>
                </div>

                <div className="form-group">
                    <label htmlFor="price">Предлагаемая цена (руб):</label>
                    <input
                        type="number"
                        id="price"
                        value={formData.price}
                        onChange={(e) => onInputChange('price', e.target.value)}
                        className="form-input"
                        placeholder="Например: 5000"
                        min="0"
                        step="100"
                    />
                </div>

                <div className="form-group">
                    <label htmlFor="description">Описание задачи:</label>
                    <textarea
                        id="description"
                        value={formData.description}
                        onChange={(e) => onInputChange('description', e.target.value)}
                        className="form-textarea"
                        placeholder="Подробно опишите вашу задачу, требования и сроки..."
                        rows="6"
                        required
                    />
                </div>

                <div className="form-summary">
                    <div className="summary-item">
                        <strong>ВУЗ:</strong> {formData.university || 'Не выбран'}
                    </div>
                    <div className="summary-item">
                        <strong>Предмет:</strong> {formData.subject || 'Не выбран'}
                    </div>
                    <div className="summary-item">
                        <strong>Категория:</strong> {categories.find(cat => cat.value === formData.category)?.label || 'Не выбрана'}
                    </div>
                    <div className="summary-item">
                        <strong>Бюджет:</strong> {formData.price ? `${formData.price} руб` : 'Не указан'}
                    </div>
                </div>

                <button type="submit" className="submit-button" disabled={isLoading}>
                    {isLoading ? 'Создание...' : 'Создать заказ'}
                </button>
            </form>
        </div>
    );
};


const CurrentOrdersTab = ({ orders, onCancelOrder, isLoading, formatDate, formatBudget }) => {
    if (isLoading) return <div className="loading">Загрузка заказов...</div>;
    if (orders.length === 0) return <div className="no-orders"><h3>Мои заказы</h3><p>У вас пока нет активных заказов</p></div>;

    return (
        <div className="current-orders-tab">
            <h3>Мои заказы ({orders.length})</h3>
            <div className="orders-list">
                {orders.map(order => (
                    <div key={order.id} className="order-card">
                        <div className="order-header">
                            <h4>{order.subject} - {order.university}</h4>
                            <span className={`status ${order.status.replace(' ', '-')}`}>{order.status}</span>
                        </div>
                        <div className="order-details">
                            <div className="detail">
                                <label>Категория:</label>
                                <span>{order.category}</span> {/* Отображаем как есть */}
                            </div>
                            <div className="detail">
                                <label>Описание:</label>
                                <p>{order.description}</p>
                            </div>
                            <div className="detail">
                                <label>Бюджет:</label>
                                <span className="budget-amount">{formatBudget(order.price)}</span>
                            </div>
                            <div className="detail">
                                <label>Создан:</label>
                                <span>{formatDate(order.createdAt)}</span>
                            </div>
                        </div>
                        <div className="order-actions">
                            <button className="action-button">Подробнее</button>
                            {order.status === 'в поиске' && (
                                <button className="action-button cancel" onClick={() => onCancelOrder(order.id)}>
                                    Отменить
                                </button>
                            )}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

const PastOrdersTab = ({ orders, isLoading, formatDate, formatBudget }) => {
    if (isLoading) return <div className="loading">Загрузка заказов...</div>;
    if (orders.length === 0) return <div className="no-orders"><h3>Прошлые заказы</h3><p>У вас пока нет завершенных заказов</p></div>;

    return (
        <div className="past-orders-tab">
            <h3>Прошлые заказы ({orders.length})</h3>
            <div className="orders-list">
                {orders.map(order => (
                    <div key={order.id} className="order-card completed">
                        <div className="order-header">
                            <h4>{order.subject} - {order.university}</h4>
                            <span className="status completed">завершен</span>
                        </div>
                        <div className="order-details">
                            <div className="detail">
                                <label>Категория:</label>
                                <span>{order.category}</span> {/* Отображаем как есть */}
                            </div>
                            <div className="detail">
                                <label>Описание:</label>
                                <p>{order.description}</p>
                            </div>
                            <div className="detail">
                                <label>Бюджет:</label>
                                <span className="budget-amount">{formatBudget(order.price)}</span>
                            </div>
                            <div className="detail">
                                <label>Завершен:</label>
                                <span>{formatDate(order.completedAt)}</span>
                            </div>
                        </div>
                        <div className="order-actions">
                            <button className="action-button">Оставить отзыв</button>
                            <button className="action-button secondary">Повторить заказ</button>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default ClientDashboard;