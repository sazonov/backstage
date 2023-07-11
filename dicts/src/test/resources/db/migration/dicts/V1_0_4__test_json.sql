create table gas_consumption['Потребление газа']
(
	number['Номер'] int,
	data json,
	comments['Комментарии'] json[]
);

insert into gas_consumption values (1, '{"day":10, "night":17, "region": "Moscow"}'::json, null);
insert into gas_consumption values (2, null, array['{"userId": 17, "comment": "comment"}'::json, '{"userId": 20, "comment": "comment"}'::json]);

update gas_consumption set comments = array['{"userId": 17, "comment": "comment17"}'::json, '{"userId": 20, "comment": "comment20"}'::json]
                       where number = 1;
update gas_consumption set data = '{"day":15, "night":22, "region": "Sochi"}'::json
                       where number = 2;