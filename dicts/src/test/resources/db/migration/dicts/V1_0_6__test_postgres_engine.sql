create table objects['Обьект'] (
    name['Наименование'] text,
    description['Описание'] text,
    coordinate json,
    comments['Комментарии'] json[]
) engine = 'postgres';

create table objects_item['Элемент обьекта'] (
    name['Наименование'] text,
    object['Обьект'] text not null references objects
) engine = 'postgres';

alter table objects add column place['Расположение'] text;
alter table objects drop column place;

alter table objects_item rename column name to item_name['Наименование элемента'];

create index objectsNameIndex on objects (name) desc;
drop index objectsNameIndex on objects;

create enum objectsEnum['Енам обьектов'] for objects as ('object_enum_1', 'object_enum_2');

alter table objects_item add constraint objectsItemNameConstraint unique (item_name);

alter table objects drop enum objectsEnum;