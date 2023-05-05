create table gas_consumption['Потребление газа']
(
	number['Номер'] int,
	data json,
	comments['Комментарии'] json[]
);

insert into gas_consumption values (1, '{"day":10, "night":17, "region": "Moscow"}'::json, null);
insert into gas_consumption values (2, null, array['{"userId": 17, "comment": "comment"}'::json, '{"userId": 20, "comment": "comment"}'::json]);