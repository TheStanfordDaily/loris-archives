from transform import transform


def raw_data():
    return """{
                \"an_array\": [
                    \"a value\",
                    \"another value\"
                ],
                \"all_amendment_date\": [
                            \"22/04/2017\",
                            \"19/12/2011\"],
                \"string_field\": \"a string\",
                \"null_field\": null,
                \"image_artwork_date_from\": \"24/04/2004\",
                \"dict_field\": {
                      \"a\": \"a_value\"
                  },
                \"image_source\": \"ignored field\",
                \"int_field\": \"200\"
            }"""


def transformed_data():
    return {
        "an_array": ["a value", "another value"],
        "all_amendment_date": ["2017-04-22", "2011-12-19"],
        "string_field": "a string",
        "null_field": None,
        "image_artwork_date_from": "2004-04-24",
        "dict_field": {"a": "a_value"},
        "int_field": 200,
    }


def test_ignores_bad_fields():
    test_dict = {"data": raw_data()}
    assert transform(test_dict) == transformed_data()