create table bridge['Реестр мостов']
(
	number['Порядковый номер'] int,
	geom['Геометрия'] geo_json
);

insert into bridge values (1, '{"type":"Feature","geometry":{"type":"Polygon","coordinates":[[[55.68154659727316,37.553090921089115],[55.646161942996564,37.58087155165372]]]}}');