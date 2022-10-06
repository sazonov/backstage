alter table workflow add column checksum varchar(255);
create index ix_workflow_checksum on workflow (checksum);