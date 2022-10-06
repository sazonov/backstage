create table user['Пользователи']
(
    "type"['Тип'] text,
    last_name['Фамилия'] text,
    first_name['Имя'] text,
    middle_name['Отчество'] text,
    contract['Договор'] attachment
);

create table indebtedness['Задолженности']
(
   number['Номер'] int,
   consumption_type['Тип'] text,
   "type"['Тип'] text,
   rso['РСО'] text,
   description['Описание'] text,
   amount['Количество'] decimal,
   amount_updated['Дата обновления'] text,
   fk_user['Автор'] text not null references user,
   fk_district['Район'] text,
   organization['Организация'] text,
   comments['Комментарии'] json[]
);

-- INSERT INTO indebtedness (consumption_type, amount, comments, fk_user, fk_district, description, type, rso, number, amount_updated, organization) VALUES ('GAS', 111.22, null, 'operator_id', 'ec9325f8-96f9-4c9c-acb9-11082ec5f19a', 'Test', 'INDEBTEDNESS', 'OEK', 1000153, '2021-04-20T15:00:55.353296', 'ZHIL');