create index if not exists ix_jbpm_eventtypes_eventtypes on jbpm_eventtypes (eventtypes);
create index if not exists ix_jbpm_eventtypes_fk_proc_instance_id on jbpm_eventtypes (fk_proc_instance_id);
create index if not exists ix_jbpm_correlationkeyinfo_processinstanceid on jbpm_correlationkeyinfo (processinstanceid);
