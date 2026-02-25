package src;

import java.io.*;
import java.util.*;

public class SocialGraph {
    // 自主实现链地址法哈希表（精简版）
    public static class CustomHashMap {
        static class Entry {
            int key;
            UserInfo value;
            Entry next;
            Entry(int k, UserInfo v) { key = k; value = v; next = null; }
        }
        private Entry[] table = new Entry[16];
        private int hash(int k) { return k % 16; }

        public void put(int k, UserInfo v) {
            int idx = hash(k);
            Entry e = table[idx];
            while (e != null) {
                if (e.key == k) { e.value = v; return; }
                e = e.next;
            }
            Entry ne = new Entry(k, v);
            ne.next = table[idx];
            table[idx] = ne;
        }

        public UserInfo get(int k) {
            int idx = hash(k);
            Entry e = table[idx];
            while (e != null) {
                if (e.key == k) return e.value;
                e = e.next;
            }
            return null;
        }

        public boolean containsKey(int k) { return get(k) != null; }
    }

    // 用户信息
    public static class UserInfo {
        int userId;
        String name;
        Set<String> interests;
        UserInfo(int id, String n, String is) {
            userId = id;
            name = n;
            interests = new HashSet<>();
            if (is != null && !is.isEmpty())
                for (String s : is.split(";")) interests.add(s.trim());
        }
        public int getUserId() { return userId; }
        public String getName() { return name; }
        public Set<String> getInterests() { return interests; }
    }

    // 核心成员
    private Map<Integer, Map<Integer, Integer>> adj = new HashMap<>();
    private CustomHashMap userMap = new CustomHashMap();
    private Map<String, Set<Integer>> interestIdx = new HashMap<>();

    // 1. 添加用户
    public void addUser(int id, String name, String interests) {
        if (!userMap.containsKey(id)) {
            adj.put(id, new HashMap<>());
            UserInfo u = new UserInfo(id, name, interests);
            userMap.put(id, u);
            for (String i : u.interests)
                interestIdx.computeIfAbsent(i, k -> new HashSet<>()).add(id);
        }
    }

    // 2. 添加好友（带权重）
    public void addFriend(int u1, int u2, int w) {
        if (userMap.containsKey(u1) && userMap.containsKey(u2)) {
            adj.get(u1).put(u2, w);
            adj.get(u2).put(u1, w);
        }
    }
    public void addFriend(int u1, int u2) { addFriend(u1, u2, 1); }

    // 3. 加载CSV用户
    public void loadUsers(String path) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"));
        br.readLine(); // 跳过表头
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] p = line.split(",");
            if (p.length < 2) continue;
            try {
                int id = Integer.parseInt(p[0].trim());
                addUser(id, p[1].trim(), p.length >= 3 ? p[2].trim() : "");
            } catch (Exception e) { System.err.println("无效行: " + line); }
        }
        br.close();
    }

    // 4. 加载TXT好友
    public void loadFriends(String path) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"));
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split("\\s+");
            if (p.length < 2) continue;
            try {
                int u1 = Integer.parseInt(p[0]);
                int u2 = Integer.parseInt(p[1]);
                int w = p.length >= 3 ? Integer.parseInt(p[2]) : 1;
                addFriend(u1, u2, w);
            } catch (Exception e) { System.err.println("无效行: " + line); }
        }
        br.close();
    }

    // 5. 一度人脉
    public List<Integer> getDirect(int id) {
        if (!userMap.containsKey(id)) return new ArrayList<>();
        List<Integer> res = new ArrayList<>(adj.get(id).keySet());
        Collections.sort(res);
        return res;
    }
    public Map<Integer, Integer> getDirectWithWeight(int id) {
        return adj.getOrDefault(id, new HashMap<>());
    }

    // 6. 二度人脉（BFS）
    public List<Integer> getSecond(int id) {
        if (!userMap.containsKey(id)) return new ArrayList<>();
        Set<Integer> vis = new HashSet<>();
        Queue<Integer> q = new LinkedList<>();
        Map<Integer, Integer> depth = new HashMap<>();
        List<Integer> res = new ArrayList<>();
        q.add(id); vis.add(id); depth.put(id, 0);

        while (!q.isEmpty()) {
            int curr = q.poll();
            int d = depth.get(curr);
            if (d >= 2) continue;
            for (int f : adj.get(curr).keySet()) {
                if (!vis.contains(f)) {
                    vis.add(f);
                    depth.put(f, d + 1);
                    q.add(f);
                    if (d + 1 == 2) res.add(f);
                }
            }
        }
        Collections.sort(res);
        return res;
    }

    // 7. 社交距离（无权BFS）
    public List<Integer> getShortestPath(int s, int t) {
        if (s == t) return Collections.singletonList(s);
        if (!userMap.containsKey(s) || !userMap.containsKey(t)) return new ArrayList<>();

        Map<Integer, Integer> prev = new HashMap<>();
        Queue<Integer> q = new LinkedList<>();
        Set<Integer> vis = new HashSet<>();
        q.add(s); vis.add(s); prev.put(s, null);
        boolean found = false;

        while (!q.isEmpty() && !found) {
            int curr = q.poll();
            for (int f : adj.get(curr).keySet()) {
                if (!vis.contains(f)) {
                    vis.add(f);
                    prev.put(f, curr);
                    q.add(f);
                    if (f == t) { found = true; break; }
                }
            }
        }

        List<Integer> path = new ArrayList<>();
        if (found) {
            Integer curr = t;
            while (curr != null) {
                path.add(curr);
                curr = prev.get(curr);
            }
            Collections.reverse(path);
        }
        return path;
    }

    // 8. 智能推荐（Top-5）
    public List<Map.Entry<Integer, Integer>> recommend(int id, int topK) {
        if (!userMap.containsKey(id)) return new ArrayList<>();
        UserInfo u = userMap.get(id);
        Set<Integer> exclude = new HashSet<>(getDirect(id));
        exclude.add(id);
        Map<Integer, Integer> score = new HashMap<>();

        for (String i : u.interests) {
            Set<Integer> users = interestIdx.getOrDefault(i, new HashSet<>());
            for (int uid : users)
                if (!exclude.contains(uid))
                    score.put(uid, score.getOrDefault(uid, 0) + 1);
        }

        List<Map.Entry<Integer, Integer>> list = new ArrayList<>(score.entrySet());
        list.sort((a, b) -> !a.getValue().equals(b.getValue()) ? b.getValue() - a.getValue() : a.getKey() - b.getKey());
        int size = Math.min(topK, list.size());
        return list.subList(0, size);
    }

    // 辅助方法
    public UserInfo getUser(int id) { return userMap.get(id); }
    public Set<Integer> getAllUserIds() { return adj.keySet(); }
    public int getEdgeCount() {
        int cnt = 0;
        for (Map<Integer, Integer> e : adj.values()) cnt += e.size();
        return cnt;
    }
}