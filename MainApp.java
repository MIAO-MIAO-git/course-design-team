package src;

import src.SocialGraph.UserInfo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;

public class MainApp extends JFrame {
    private SocialGraph socialGraph;
    private JTextField userIdInput;
    private JTextArea resultArea;

    public MainApp() {
        socialGraph = new SocialGraph();
        loadData();
        initUI();
    }

    private void loadData() {
        try {
            socialGraph.loadUsers("users.csv");
            socialGraph.loadFriends("relationships.txt");
            JOptionPane.showMessageDialog(this, "数据加载成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "数据加载失败：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void initUI() {
        setTitle("社交网络分析系统");
        setSize(900, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(mainPanel);

        // 1. 顶部标题
        JLabel titleLabel = new JLabel("社交网络分析系统", SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // 2. 输入区（用户ID输入 + 下拉选择）
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        inputPanel.add(new JLabel("当前用户ID:"));
        userIdInput = new JTextField(10);
        inputPanel.add(userIdInput);

        inputPanel.add(Box.createHorizontalStrut(50)); // 分隔空间

        inputPanel.add(new JLabel("或选择用户:"));
        JComboBox<String> userSelectCombo = new JComboBox<>();
        // 加载所有用户ID到下拉框
        for (int id : socialGraph.getAllUserIds()) {
            UserInfo info = socialGraph.getUser(id);
            userSelectCombo.addItem(id + " - " + info.getName());
        }
        // 下拉框选择后自动填充到输入框
        userSelectCombo.addActionListener(e -> {
            String selected = (String) userSelectCombo.getSelectedItem();
            if (selected != null) {
                String idStr = selected.split(" - ")[0];
                userIdInput.setText(idStr);
            }
        });
        inputPanel.add(userSelectCombo);
        mainPanel.add(inputPanel, BorderLayout.NORTH);

        // 3. 功能按钮区
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JButton btnDirect = new JButton("查询直接好友");
        JButton btnSecond = new JButton("查找二度人脉");
        JButton btnDistance = new JButton("计算社交距离");
        JButton btnRecommend = new JButton("智能推荐(Top-5)");
        JButton btnClear = new JButton("清空结果");

        buttonPanel.add(btnDirect);
        buttonPanel.add(btnSecond);
        buttonPanel.add(btnDistance);
        buttonPanel.add(btnRecommend);
        buttonPanel.add(btnClear);
        mainPanel.add(buttonPanel, BorderLayout.CENTER);

        // 4. 结果展示区（选项卡，仅保留分析结果和统计信息）
        JTabbedPane tabbedPane = new JTabbedPane();

        // 分析结果页
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        // 显示初始加载信息
        resultArea.setText("数据加载成功！\n已加载 " + socialGraph.getAllUserIds().size() + " 名用户信息。\n已加载 " + (socialGraph.getEdgeCount() / 2) + " 组好友关系。");
        JScrollPane resultScroll = new JScrollPane(resultArea);
        resultPanel.add(resultScroll, BorderLayout.CENTER);
        tabbedPane.addTab("分析结果", resultPanel);

        // 统计信息页
        JPanel statsPanel = new JPanel(new BorderLayout());
        JTextArea statsArea = new JTextArea();
        statsArea.setEditable(false);
        statsArea.setText("=== 统计信息 ===\n用户总数: " + socialGraph.getAllUserIds().size() + "\n好友关系总数: " + (socialGraph.getEdgeCount() / 2));
        statsPanel.add(new JScrollPane(statsArea), BorderLayout.CENTER);
        tabbedPane.addTab("统计信息", statsPanel);

        mainPanel.add(tabbedPane, BorderLayout.SOUTH);

        // 绑定按钮事件
        btnDirect.addActionListener(this::queryDirectFriends);
        btnSecond.addActionListener(this::querySecondFriends);
        btnDistance.addActionListener(this::calculateDistance);
        btnRecommend.addActionListener(e -> {
            Integer userId = getInputUserId();
            if (userId != null) {
                List<Map.Entry<Integer, Integer>> recommends = socialGraph.recommend(userId, 5);
                StringBuilder sb = new StringBuilder();
                sb.append("=== 智能推荐（用户ID：").append(userId).append("，Top5）===\n");
                if (recommends.isEmpty()) {
                    sb.append("暂无推荐好友！");
                } else {
                    int rank = 1;
                    for (Map.Entry<Integer, Integer> entry : recommends) {
                        int rid = entry.getKey();
                        int score = entry.getValue();
                        UserInfo info = socialGraph.getUser(rid);
                        sb.append("  第").append(rank).append("名 | ID：").append(rid).append(" | 姓名：").append(info.getName()).append(" | 共同兴趣数：").append(score).append("\n");
                        rank++;
                    }
                }
                resultArea.setText(sb.toString());
            }
        });
        btnClear.addActionListener(e -> resultArea.setText(""));
    }

    private Integer getInputUserId() {
        String text = userIdInput.getText().trim();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入用户ID！", "提示", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        try {
            int userId = Integer.parseInt(text);
            if (!socialGraph.getAllUserIds().contains(userId)) {
                JOptionPane.showMessageDialog(this, "用户ID不存在！", "提示", JOptionPane.WARNING_MESSAGE);
                return null;
            }
            return userId;
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "请输入有效的数字ID！", "提示", JOptionPane.WARNING_MESSAGE);
            return null;
        }
    }

    private void queryDirectFriends(ActionEvent e) {
        Integer userId = getInputUserId();
        if (userId == null) return;

        List<Integer> friends = socialGraph.getDirect(userId);
        StringBuilder sb = new StringBuilder();
        sb.append("=== 一度人脉查询（用户ID：").append(userId).append("）===\n");
        sb.append("直接好友总数：").append(friends.size()).append("\n\n");
        for (int fid : friends) {
            UserInfo info = socialGraph.getUser(fid);
            int weight = socialGraph.getDirectWithWeight(userId).get(fid);
            sb.append("  ID：").append(fid).append(" | 姓名：").append(info.getName()).append(" | 亲密度：").append(weight).append("\n");
        }
        resultArea.setText(sb.toString());
    }

    private void querySecondFriends(ActionEvent e) {
        Integer userId = getInputUserId();
        if (userId == null) return;

        List<Integer> friends = socialGraph.getSecond(userId);
        StringBuilder sb = new StringBuilder();
        sb.append("=== 二度人脉查询（用户ID：").append(userId).append("）===\n");
        sb.append("二度人脉总数：").append(friends.size()).append("\n\n");
        for (int fid : friends) {
            UserInfo info = socialGraph.getUser(fid);
            sb.append("  ID：").append(fid).append(" | 姓名：").append(info.getName()).append("\n");
        }
        resultArea.setText(sb.toString());
    }

    private void calculateDistance(ActionEvent e) {
        Integer startId = getInputUserId();
        if (startId == null) return;

        String targetText = JOptionPane.showInputDialog(this, "请输入目标用户ID：");
        if (targetText == null || targetText.trim().isEmpty()) return;

        try {
            int targetId = Integer.parseInt(targetText.trim());
            List<Integer> path = socialGraph.getShortestPath(startId, targetId);

            StringBuilder sb = new StringBuilder();
            sb.append("=== 社交距离计算（").append(startId).append(" → ").append(targetId).append("）===\n");
            if (path.isEmpty()) {
                sb.append("⚠️  两个用户之间无连通路径！");
            } else {
                sb.append("最短社交距离：").append(path.size() - 1).append("\n");
                sb.append("路径：");
                for (int i = 0; i < path.size(); i++) {
                    int pid = path.get(i);
                    UserInfo info = socialGraph.getUser(pid);
                    sb.append(pid).append("(").append(info.getName()).append(")");
                    if (i < path.size() - 1) sb.append(" → ");
                }
            }
            resultArea.setText(sb.toString());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "目标ID请输入有效数字！", "提示", JOptionPane.WARNING_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainApp app = new MainApp();
            app.setVisible(true);
        });
    }
}
//加一行注释谢谢