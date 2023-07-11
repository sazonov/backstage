create table user['Пользователи']
(
    "type"['Тип'] text,
    last_name['Фамилия'] text,
    first_name['Имя'] text,
    middle_name['Отчество'] text,
    contract['Договор'] attachment,
    birth_date['Дата рождения'] date
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
