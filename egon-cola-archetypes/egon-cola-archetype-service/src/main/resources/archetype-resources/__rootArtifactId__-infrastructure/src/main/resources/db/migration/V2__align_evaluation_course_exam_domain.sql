alter table course add column code varchar(96);
update course set code = 'LEGACY-' || id where code is null;
alter table course alter column code set not null;
create unique index uk_course_code on course (code);

create table course_schedule (
    id varchar(64) primary key,
    course_id varchar(64) not null,
    class_id varchar(64) not null,
    starts_at timestamp not null,
    ends_at timestamp not null,
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_schedule_course foreign key (course_id) references course (id),
    constraint ck_schedule_window check (starts_at < ends_at)
);
create index idx_schedule_overlap
    on course_schedule (course_id, class_id, starts_at, ends_at);

create table exam (
    id varchar(160) primary key,
    course_id varchar(64) not null,
    title varchar(128) not null,
    starts_at timestamp not null,
    ends_at timestamp not null,
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_exam_course foreign key (course_id) references course (id),
    constraint ck_exam_window check (starts_at < ends_at)
);
create index idx_exam_course_created on exam (course_id, created_at, id);

create table exam_paper (
    id varchar(160) primary key,
    exam_id varchar(160) not null,
    title varchar(128) not null,
    total_points integer not null,
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_paper_exam foreign key (exam_id) references exam (id),
    constraint ck_paper_points check (total_points > 0)
);
create unique index uk_exam_paper_exam on exam_paper (exam_id);

create table score (
    id varchar(64) primary key,
    exam_id varchar(160) not null,
    course_id varchar(64) not null,
    student_id varchar(64) not null,
    points integer not null,
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_score_exam foreign key (exam_id) references exam (id),
    constraint fk_score_course foreign key (course_id) references course (id),
    constraint ck_score_points check (points between 0 and 100),
    constraint uk_score_exam_student unique (exam_id, student_id)
);
create index idx_score_exam_created on score (exam_id, created_at, id);
create index idx_score_course on score (course_id);
create index idx_score_student on score (student_id);

insert into exam (id, course_id, title, starts_at, ends_at, status, created_at, updated_at)
select 'legacy-exam-' || id,
       course_id,
       'Legacy exam ' || id,
       created_at,
       case when updated_at > created_at
            then updated_at
            else created_at + interval '1' second
       end,
       'CLOSED',
       created_at,
       updated_at
from exam_result;

insert into exam_paper (id, exam_id, title, total_points, status, created_at, updated_at)
select 'legacy-paper-' || id,
       'legacy-exam-' || id,
       'Legacy paper ' || id,
       100,
       'PUBLISHED',
       created_at,
       updated_at
from exam_result;

insert into score (id, exam_id, course_id, student_id, points, status, created_at, updated_at)
select id,
       'legacy-exam-' || id,
       course_id,
       student_id,
       score,
       status,
       created_at,
       updated_at
from exam_result;
