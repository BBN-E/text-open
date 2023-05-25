import json
import requests


def get_wikidata_affiliation(qid, affiliation_property="P27"):
    '''
    :param qid: claimer QID (e.g. "Q15031" for "Xi Jinping")
    :param affiliation_property: P27="country of citizenship"
    :return: qid for affiliation
    '''

    sparql_query = (
        'SELECT DISTINCT ?item ?itemLabel ?itemDescription ?sitelinks'
        'WHERE {'
        # f'  wd:{qid} wdt:P31/wdt:P279* wd:Q5.'  # qid is a subclass of a human
        f'  wd:{qid} ( wdt:{affiliation_property} ) ?item;'
        '               wikibase:sitelinks ?sitelinks.'
        '  SERVICE wikibase:label { bd:serviceParam wikibase:language "en,en-ca,en-gb,en-us" }'
        '}'
        'ORDER BY DESC(?sitelinks)'
    )
    res = requests.get("https://query.wikidata.org/sparql", params={"query": sparql_query, "format": "json"})
    # print(json.dumps(res, indent=4, sort_keys=True))
    if res.status_code != 200:
        return None, None

    res_json = res.json()

    affiliation_qid, affiliation_label = None, None
    if len(res_json["results"]["bindings"]) > 0:
        affiliation_qid = res_json["results"]["bindings"][0]["item"]["value"]
        affiliation_label = res_json["results"]["bindings"][0]["itemLabel"]["value"]

    if affiliation_qid is not None:
        return affiliation_qid.split("/")[-1], affiliation_label
    else:
        return None, None


# if __name__ == '__main__':
#     affiliation_qid, affiliation_label = get_wikidata_affiliation(qid="Q15068053")
#     print(affiliation_qid, affiliation_label)
