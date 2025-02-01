delete from public.act_ru_job;
delete from public.act_ru_timer_job;
delete from public.act_ru_suspended_job;
delete from public.act_ru_external_job;
delete from public.act_ru_deadletter_job;
delete from public.act_ru_task;
delete from public.act_ru_variable;
delete from public.act_ru_identitylink;
delete from public.act_ru_event_subscr where execution_id_ IS NOT NULL;
delete from public.act_ru_execution;
delete from public.act_ru_actinst;

delete from public.act_hi_actinst;
delete from public.act_hi_attachment;
delete from public.act_hi_comment;
delete from public.act_hi_detail;
delete from public.act_hi_entitylink;
delete from public.act_hi_identitylink;
delete from public.act_hi_procinst;
delete from public.act_hi_taskinst;
delete from public.act_hi_taskinst;
delete from public.act_hi_tsk_log;
delete from public.act_hi_varinst;

