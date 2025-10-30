import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App.jsx'
import './CSS/index.css'
import './CSS/index.css'
import 'primereact/resources/themes/lara-dark-cyan/theme.css';
import 'primereact/resources/primereact.min.css';
import 'primeicons/primeicons.css';
import { PrimeReactProvider } from 'primereact/api';

ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
        <PrimeReactProvider>
            <BrowserRouter>
                <App />
            </BrowserRouter>
        </PrimeReactProvider>
    </React.StrictMode>,
)