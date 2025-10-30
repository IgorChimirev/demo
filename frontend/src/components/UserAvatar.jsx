import React from 'react';
import './CSS/UserAvatar.css';

const UserAvatar = ({ user, isHidden, onClick }) => {
    if (!user) return null;

    let avatarContent;
    let avatarStyle = {};

    if (user.photo_url) {
        avatarStyle.backgroundImage = `url(${user.photo_url})`;
        avatarContent = null;
    } else {
        const firstNameLetter = user.first_name ? user.first_name[0] : '';
        const lastNameLetter = user.last_name ? user.last_name[0] : '';
        avatarContent = (firstNameLetter + lastNameLetter).toUpperCase();
    }

    return (
        <div
            className={`channel-avatar-Profil ${isHidden ? 'hidden' : ''}`}
            style={avatarStyle}
            onClick={onClick}
        >
            {avatarContent}
        </div>
    );
};

export default UserAvatar;