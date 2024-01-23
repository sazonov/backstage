/* Роли */
/* Роли */
create table roles['Роли'] (name['Название'] text, comments['Комментарии'] text[]);

create table useless['Для удаления'] (field['Название'] text);

-- Пользователи.
create table users['Пользователи'] (
									   name['Имя'] text,
									   age['Возраст'] int,
									   role['Роль'] text not null references roles,
									   registrationDate['Дата регистрации'] date);

alter table users add column comments['Комментарии'] text[];
alter table roles drop column comments;

insert into roles (id, name) values ('MANAGER', 'Менеджер'), ('MANAGER1', 'Менеджер1'), ('MANAGER2', 'Менеджер2');
insert into users values('Иван', 20, 'MANAGER', '2020-08-20', ARRAY['11', '22', '33']);

update roles set name = 'Супер-Менеджер' where id = 'MANAGER';

delete from roles where id = 'MANAGER';
delete from roles where name = 'Менеджер2';

alter table users rename column age to age['Только обновили описание'];
alter table users rename column age to ages['Возраст'];
alter table users rename column ages to age;

drop table useless;

create index usersNameIndex on users (name);
drop index usersNameIndex on users;
create index usersNameIndex on users (name);

alter table users add constraint usersNameConstraint unique (age);
alter table users add constraint usersNameConstraint1 unique (id, name);
alter table users drop constraint usersNameConstraint;

alter table users set readPermission = 'DICT_READ';
alter table users set writePermission = null;

create enum sthEnum for users as ('value1', 'value2');
alter table users alter enum sthEnum add value 'value3';
alter table users drop enum sthEnum;

create table enumTest (name text);
create enum sthEnum['енам'] for enumTest as ('value1', 'value2');
alter table enumTest add column enumField sthEnum;
insert into enumTest values('Тест', 'value1');
drop table enumTest;