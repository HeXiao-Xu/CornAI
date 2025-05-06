// 加载账户信息
document.addEventListener('DOMContentLoaded', () => {
    fetch('/api/account/info', {
        headers: { 'X-User-ID': localStorage.getItem('userId') }
    })
        .then(response => response.json())
        .then(data => {
            document.getElementById('phoneNumber').textContent = data.phoneNumber;
            document.getElementById('themeSelect').value = data.theme;
        });
});

// 更新主题
function updateTheme(theme) {
    fetch('/api/account/theme', {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-User-ID': localStorage.getItem('userId')
        },
        body: `theme=${theme}`
    });
}