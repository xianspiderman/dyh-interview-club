INSERT INTO club_category(id, parent_id, name, sort_no, status) VALUES
  (1, NULL, 'Java', 1, 'ENABLED'),
  (2, 1, '集合框架', 1, 'ENABLED'),
  (3, 1, '并发编程', 2, 'ENABLED'),
  (4, 1, 'JVM', 3, 'ENABLED');

INSERT INTO club_label(id, category_id, name, status) VALUES
  (1, 2, 'HashMap', 'ENABLED'),
  (2, 2, 'ConcurrentHashMap', 'ENABLED'),
  (3, 3, 'ThreadLocal', 'ENABLED'),
  (4, 3, 'AQS', 'ENABLED'),
  (5, 4, '垃圾回收', 'ENABLED');

INSERT INTO club_circle(id, parent_id, name, sort_no, status) VALUES
  (1, NULL, 'Java 圈子', 1, 'ENABLED'),
  (2, 1, '求职交流圈', 1, 'ENABLED'),
  (3, 1, '系统设计圈', 2, 'ENABLED'),
  (4, 1, '性能优化圈', 3, 'ENABLED');

INSERT INTO club_sensitive_word(id, word_text, list_type, status, dictionary_version) VALUES
  (1, '非法广告', 'BLACK', 'ENABLED', 1),
  (2, '广告推荐算法', 'WHITE', 'ENABLED', 1);
