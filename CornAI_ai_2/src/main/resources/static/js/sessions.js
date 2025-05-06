// 加载会话列表
document.addEventListener('DOMContentLoaded', () => {
    fetch('/api/chat/sessions', {
        headers: { 'X-User-ID': localStorage.getItem('userId') }
    })
        .then(response => response.json())
        .then(sessions => {
            const listDiv = document.getElementById('sessionManageList');
            listDiv.innerHTML = sessions.map(session => `
            session-item">
                ${session.name}</span>
                <button onclick="deleteSession('${session.sessionId}')">删除</button>
            </div>
        `).join('');
        });
});

// // 创建会话
// function createSession() {
//     const name = document.getElementById('sessionNameInput').value.trim();
//     fetch('/api/sessions', {
//         method: 'POST',
//         headers: {
//             'Content-Type': 'application/json',
//             'X-User-ID': localStorage.getItem('userId')
//         },
//         body: JSON.stringify({ name: name || "新建会话" }) // 处理空名称
//     })
//         .then(response => {
//             if (!response.ok) {
//                 return response.text().then(text => { throw new Error(text) });
//             }
//             return response.json();
//         })
//         .then(data => {
//             console.log("创建成功:", data);
//             loadSessions();
//         })
//         .catch(error => {
//             console.error("完整错误:", error);
//             alert(`创建失败: ${error.message.includes("502") ? "AI服务不可用" : error.message}`);
//         });
// }

// 删除会话
function deleteSession(sessionId) {
    if (!confirm('确定要删除此会话吗？')) return;

    fetch(`/api/sessions/${sessionId}`, {
        method: 'DELETE',
        headers: { 'X-User-ID': localStorage.getItem('userId') }
    })
        .then(response => {
            if (response.ok) window.location.reload();
        });
}