// 登录处理
function handleLogin(e) {
    e.preventDefault();
    const phone = document.getElementById('phone').value;
    const password = document.getElementById('password').value;

    fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: `phoneNumber=${encodeURIComponent(phone)}&password=${encodeURIComponent(password)}`
    })
        .then(response => {
            if (response.status === 200) {
                return response.text().then(userId => {
                    localStorage.setItem('userId', userId);
                    window.location.href = '/chat';
                });
            } else {
                return response.text().then(errorMessage => {
                    // 根据后端返回的错误信息，给出具体提示
                    if (errorMessage === "用户不存在") {
                        alert('登录失败: 用户不存在');
                    } else if (errorMessage === "密码错误") {
                        alert('登录失败: 密码错误');
                    } else {
                        alert('登录失败: ' + errorMessage);
                    }
                });
            }
        })
        .catch(error => console.error('Error:', error));
}

// 注册处理
function handleRegister(e) {
    e.preventDefault();
    const phone = document.getElementById('regPhone').value;
    const password = document.getElementById('regPassword').value;

    if (phone.length != 11) {
        alert('手机号必须为11位');
        return;
    }

    fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: `phoneNumber=${encodeURIComponent(phone)}&password=${encodeURIComponent(password)}`
    })
        .then(response => {
            if (response.ok) {
                alert('注册成功，请登录');
                window.location.href = '/login';
            } else {
                response.text().then(text => alert('注册失败: ' + text));
            }
        })
        .catch(error => console.error('Error:', error));
}