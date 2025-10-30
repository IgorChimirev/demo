import React from 'react';
import './CSS/HamburgerMenu.css';

const HamburgerMenu = ({ isActive, onToggle }) => {
    return (
        <div
            className={`hamburger-menu ${isActive ? 'hidden' : ''}`}
            onClick={onToggle}
        >
            <span></span>
            <span></span>
            <span></span>
        </div>
    );
};

export default HamburgerMenu;

