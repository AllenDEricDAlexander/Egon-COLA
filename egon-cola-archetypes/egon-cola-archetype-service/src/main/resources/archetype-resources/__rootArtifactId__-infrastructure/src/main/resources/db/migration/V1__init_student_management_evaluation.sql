create table course (
    id varchar(64) primary key,
    name varchar(128) not null,
    credit integer not null,
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index uk_course_name on course (name);

create table exam_result (
    id varchar(64) primary key,
    course_id varchar(64) not null,
    student_id varchar(64) not null,
    score integer not null,
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_exam_result_course_id on exam_result (course_id);
create index idx_exam_result_student_id on exam_result (student_id);
