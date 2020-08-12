mysql -u root

CREATE DATABASE test;
create user chk386@'%' identified by 'Cdr0m38^';
GRANT ALL PRIVILEGES ON test.* TO 'chk386'@'%';

drop table commit_log;
create table commit_log
(
    no int auto_increment
        primary key,
    author varchar(100) not null,
    month numeric not null,
    url varchar(500) not null,
    title varchar(3000) not null,
    modified_line_count numeric not null,
    deleted_line_count numeric not null,
    file_changed_count numeric not null,
    file_changed_name_compressed varchar(800)
);

create index commit_log_idx
    on commit_log (author, month, modified_line_count, file_changed_count, file_changed_name_compressed);

select a.author, if(grouping(a.month), '월합계', a.month) as monnth
     , sum(a.modified_line_count) as modified
     , sum(a.deleted_line_count) as deleted
     , sum(a.file_changed_count) as changed
  from (
        select row_number() over (partition by file_changed_name_compressed order by a.no desc) as ranking, a.no, a.author, a.month, a.url, a.title, a.modified_line_count, a.deleted_line_count, a.file_changed_count,
               file_changed_name_compressed
         from commit_log a
        where modified_line_count != 0 and deleted_line_count != 0 and file_changed_count != 0 and modified_line_count <= 10000
          and a.author not in ('dlwoen9', 'haky2', 'hyunkook-lee', 'joonhyung-seo', 'kyuseok', 'cjb3333', 'endy', 'hidefx', 'hyeripark', 'jaychoi', 'jhcho84', 'kyuseok', 'pet2r', 'pkhyeri', 'sunge0314', '박재성', '송경민')
       ) a
 where a.ranking = 1
 group by a.author, a.month with rollup
 order by author, month desc;

select distinct author from commit_log;
select * from commit_log where author = 'JP17067';


# JP17067 : 조원기
# 畠山 魁: 하타케야마

# delete from commit_log where no > 0;
# delete from commit_log where file_changed_name_compressed = '';
select count(*) from commit_log where file_changed_name_compressed != '';

commit ;

select distinct author from commit_log;


CREATE TEMPORARY TABLE t1 (
    select a.* from commit_log a
);

update t1
   set author = 'wonki-cho'
 where author in ('CHO WONKI', 'JP17067');

update t1
   set author = 'kai-hatakeyama'
 where author in ('畠山 魁/TEMPO Cloud開発チーム/JP');

update t1
   set author = 'jongdoo-jung'
 where author in ('JUNG JONGDOO');


update t1
   set author = 'bareun-kim'
 where author in ('金バルン/ECクラウド開発部/JP');

commit;

select a.author, if(grouping(a.month), '월합계', a.month) as monnth
     , sum(a.modified_line_count) as modified
     , sum(a.deleted_line_count) as deleted
     , sum(a.file_changed_count) as changed
from (
         select row_number() over (partition by file_changed_name_compressed order by a.no desc) as ranking, a.no, a.author, a.month, a.url, a.title, a.modified_line_count, a.deleted_line_count, a.file_changed_count,
                file_changed_name_compressed
         from t1 a
         where modified_line_count != 0 and deleted_line_count != 0 and file_changed_count != 0 and modified_line_count <= 10000
           and a.author not in ('dlwoen9', 'haky2', 'hyunkook-lee', 'joonhyung-seo', 'kyuseok', 'cjb3333', 'endy', 'hidefx', 'hyeripark', 'jaychoi', 'jhcho84', 'kyuseok', 'pet2r', 'pkhyeri', 'sunge0314', '박재성', '송경민')
     ) a
where a.ranking = 1
group by a.author, a.month with rollup
order by author, month desc;

select * from t1 order by no asc;


