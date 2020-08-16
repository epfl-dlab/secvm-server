INSERT INTO feature_type VALUES ('hosts'), ('titleWords');

INSERT INTO dice_roll VALUES (NULL, 'PwAAAD8AAAA='/*[0.5, 0.5]*/), (NULL, 'P2ZmZj3MzM0='/*[0.9, 0.1]*/), (NULL, 'P4AAAAAAAAA='/*[1, 0]*/);

INSERT INTO feature_vector VALUES (NULL, 'hosts', 15, 1), (NULL, 'hosts', 8, 2), (NULL, 'titleWords', 10, 1);

INSERT INTO svm VALUES (NULL, 'test_name_1', 0.1, 4, 500, 1, 'AAAAAQAAAAIAAAAF'/*[1, 2, 5]*/, 'AAAAAAAAAAMAAAAE'/*[0, 3, 4]*/, 1, 1),
  (NULL, 'test_name_2', 0.05, 4, 350, 3, 'AAAAAA=='/*[0]*/, 'AAAAAQ=='/*[1]*/, 1, 0);

INSERT INTO svm_uses_feature_vector VALUES (1, 2), (1, 3), (2, 2);





INSERT INTO weight_vector VALUES (1, 0, NOW(6), NOW(6), 0, 'AAAAAAAAAAAAAAAAAAAAAA=='/*[0, 0, 0, 0]*/, NULL/*NULL*/),
  (1, 1, NOW(6), NOW(6) + INTERVAL 10000 MICROSECOND, 541, 'P0euFD8tkWg/4mtRQOAAAA=='/*[0.78, 0.678, 1.7689, 7]*/, NULL/*NULL*/),
  (1, 2, NOW(6) + INTERVAL 10000 MICROSECOND, NOW(6) + INTERVAL 456000 MICROSECOND, 501, 'QMAAAD+AAABB0AAARAzAAA=='/*[6, 1, 26, 563]*/, 'NULL'/*NULL*/),
  (1, 3, NOW(6) + INTERVAL 470000 MICROSECOND, NULL, 700, 'QAAAAEEI9cNC9gAAQDhPtQ=='/*[2, 8.56, 123, 2.879865]*/, 'AAARlQAAAiL//+8JAAC/wg=='/*[4501, 546, -4343, 49090]*/),
  (2, 0, NOW(6), NOW(6), 0, 'AAAAAAAAAAAAAAAAAAAAAA=='/*[0, 0, 0, 0]*/, NULL/*NULL*/);

INSERT INTO test_accuracy VALUES (1, 0, 84, 90, 40, 48), (1, 1, 100, 132, 70, 98);

/*
[0, 0, 0, 0] -> AAAAAAAAAAAAAAAAAAAAAA==
*/
