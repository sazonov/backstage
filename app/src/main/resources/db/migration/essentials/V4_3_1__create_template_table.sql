create table if not exists document_template
(
	id                  varchar(40)         primary key,
    name                varchar(500)        not null,
    fk_attachment		varchar(40)         not null references attachment (id),
    created             timestamp           not null,
    updated             timestamp           not null,
    deleted             timestamp
);

create extension if not exists pg_trgm;

create index ix_fk_document_template_fk_attachment
    on document_template (fk_attachment);

create index ix_document_template_name_lower_trigram
    on document_template using gin (lower(name) gin_trgm_ops);