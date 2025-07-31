The files included in this folder represent the json used for the Query Annotations in
RecommendationElasticSearchRepository, to be used for testing against a running ES instance.

Copy these queries into a minifier for use in the repository class @Query annotations

## Hint - reading ES queries
Look out for the "bool" clauses in the query, particularly "should" and "filter".

It's sometimes useful to view these in terms of traditional logical clauses.

Every clause in a "should": [] can be considered to be logically "OR'ed" together.

Every clause in a "filter": [] can be considered to be logically "AND'ed" together.

e.g. 
```json
{
  "should": [
    {
      "match": <match clause A>
    },
    {
      "match": <match clause B>
    }
  ]
}
```
Can be read as "Match A OR Match B"

e.g.
```json
{
  "filter": [
    {
      "match": <match clause A>
    },
    {
      "match": <match clause B>
    }
  ]
}
```
Can be read as "Match A AND Match B"

You also can nest the clauses, such as a "filter" nested in a "should" can be used to produce the equivalent of A OR (B AND C)

This is not the intended way of thinking about Elasticsearch queries, but for our use case of combining arbitrary filters it can be quite useful.
