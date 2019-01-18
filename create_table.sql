drop table IF EXISTS lettre;

create table lettre (
id integer,
score numeric,
sentence varchar(500),
nb_words integer
);

COPY lettre(id,score,sentence, nb_words) 
FROM 'C:\tmp\backup_sentiments-scores.csv' DELIMITER ',' CSV ESCAPE '"';

alter table lettre add column score_cat integer;

update lettre set score_cat = 0 where score < 0.1; 
update lettre set score_cat = 1 where score >= 0.1 and score < 0.2; 
update lettre set score_cat = 2 where score >= 0.2 and score < 0.3; 
update lettre set score_cat = 3 where score >= 0.3 and score < 0.4; 
update lettre set score_cat = 4 where score >= 0.4 and score < 0.5; 
update lettre set score_cat = 5 where score >= 0.5 and score < 0.6; 
update lettre set score_cat = 6 where score >= 0.6 and score < 0.7; 
update lettre set score_cat = 7 where score >= 0.7 and score < 0.8; 
update lettre set score_cat = 8 where score >= 0.8 and score < 0.9; 
update lettre set score_cat = 9 where score >= 0.9 and score <= 1; 

alter table lettre add column sentiment_flag integer;
update lettre set sentiment_flag = 0;
update lettre set sentiment_flag = 1 where score_cat >= 5;


select score_cat, count(*) as "nb hits"
from lettre
group by score_cat
order by score_cat desc;


select sentiment_flag, count(*) 
from lettre
group by sentiment_flag;

alter table lettre add column sentence_length integer;
update lettre set sentence_length=char_length(sentence);

alter table lettre add column nb_words_cat integer;
update lettre set nb_words_cat = 0 where nb_words >=0 and nb_words < 10;
update lettre set nb_words_cat = 1 where nb_words >=10 and nb_words < 20;
update lettre set nb_words_cat = 2 where nb_words >=20 and nb_words < 30;
update lettre set nb_words_cat = 3 where nb_words >=30 and nb_words < 40;
update lettre set nb_words_cat = 4 where nb_words >=40 and nb_words < 50;
update lettre set nb_words_cat = 5 where nb_words >=50 and nb_words < 60;


COPY lettre TO '/tmp/lettre_macron_2019_compiled.csv' WITH (FORMAT CSV, HEADER);