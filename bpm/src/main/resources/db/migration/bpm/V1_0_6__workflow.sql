create table workflow (
    id varchar(255) not null,
    created timestamp not null,
    definition text not null,

    primary key (id));