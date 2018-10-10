# -*- encoding: utf-8 -*-
"""
This file contains the logic for sorting images into their final destination.

It only has a single public function: ``sort_image``, which takes a Python
dictionary and returns an instance of ``Decision``.
"""

import dateutil.parser
import enum
import re


class Rules:
    def __init__(self, collection, image_data):
        self.collection = collection
        self.image_data = image_data

    _empty_title_strings = [
        "-",
        "--",
        "Awaiting description",
        "Awaiting removal from MIRO as duplicate",
        "Awaiting captions and consent form",
        "Awaiting catalogue details",
        "Awaiting caption info",
        "No info available about this object",
        "No neg",
    ]

    _cc_accesses = ["CC-0", "CC-BY", "CC-BY-NC", "CC-BY-NC-ND"]

    @staticmethod
    def _normalise_string(s):
        if s is not None:
            s = s.lower().strip()
        if s == "":
            s = None
        return s

    def _get(self, key):
        return self.image_data.get(key)

    def _get_normalised(self, key):
        return self._normalise_string(self._get(key))

    def _compare_normalised(self, a, b):
        return self._normalise_string(a) == self._normalise_string(b)

    def _compare(self, key, value):
        key_values = self._get(key)

        if not isinstance(key_values, list):
            key_values = [key_values]

        return any(
            self._compare_normalised(key_value, value) for key_value in key_values
        )

    def _is_collection(self, collection_name):
        return self._normalise_string(self.collection) == self._normalise_string(
            f"images-{collection_name}"
        )

    def _search(self, regex, key):
        if self._get_normalised(key) is None:
            return None
        else:
            return re.search(regex, self._get_normalised(key))

    def _is_blank(self, key):
        return self._get_normalised(key) is None

    def _key_matches(self, key, string_list):
        return any([self._compare(key, string) for string in string_list])

    def _parse_date(self, date_string):
        return dateutil.parser.parse(date_string, dayfirst=True)

    def is_collection(self, *args):
        return any(self._is_collection(collection) for collection in args)

    @property
    def image_library_dept_is_Archives_and_Manuscripts(self):
        return self._compare(
            "image_library_dept", "Archives and Manuscripts"
        ) or self._compare("image_library_dept", "Archives & Manuscripts")

    @property
    def image_library_dept_is_Public_programmes(self):
        return self._compare("image_library_dept", "Public programmes")

    @property
    def image_tech_captured_mode_is_videodisc(self):
        return self._compare("image_tech_captured_mode", "videodisc")

    @property
    def is_innopac_id_8_digits(self):
        return self._search(r"[0-9]{7}[0-9xX]{1}", "image_innopac_id") is not None

    @property
    def is_title_blank(self):
        return self._is_blank("image_title")

    @property
    def is_title_empty(self):
        return self.is_title_blank or self._key_matches(
            "image_title", self._empty_title_strings
        )

    @property
    def is_image_pub_title_blank(self):
        return self._is_blank("image_pub_title")

    @property
    def is_image_pub_periodical_blank(self):
        return self._is_blank("image_pub_periodical")

    @property
    def is_after_first_march_2016(self):
        scanned_date = self._get("image_tech_scanned_date")
        if scanned_date is not None:
            return self._parse_date(scanned_date) > self._parse_date("1 March 2016")
        else:
            return False

    @property
    def is_copyright_cleared(self):
        return self._get("image_copyright_cleared") == "Y"

    @property
    def is_not_general_use(self):
        return self._get("image_general_use") == "N"

    @property
    def has_use_restrictions(self):
        return not self._key_matches("image_use_restrictions", self._cc_accesses)

    @property
    def is_not_for_public_access(self):
        return (
            (not self.is_copyright_cleared)
            or self.is_not_general_use
            or self.has_use_restrictions
        )

    @property
    def is_for_public_access(self):
        return not self.is_not_for_public_access

    @property
    def is_a_wellcome_image_awards_winner(self):
        return self._key_matches(
            "image_award", ["Biomedical Image Awards", "Wellcome Image Awards"]
        )

    @property
    def is_cold_store(self):
        return (
            self.is_collection("D", "F", "AS", "FP")
            or (
                self.is_collection("L", "M", "V")
                and self.image_library_dept_is_Archives_and_Manuscripts
            )
            or (
                self.is_collection("L", "M", "V")
                and self.image_tech_captured_mode_is_videodisc
            )
            or (
                self.is_collection("L", "M", "V")
                and not self.is_innopac_id_8_digits
                and self.is_title_empty
                and self.is_image_pub_title_blank
                and self.is_image_pub_periodical_blank
            )
        )

    @property
    def is_tandem_vault(self):
        return (
            self.image_library_dept_is_Public_programmes
            or self.is_collection("L")
            and self.is_after_first_march_2016
            or self.is_collection("L", "M", "V")
            and self.is_not_for_public_access
            or self.is_a_wellcome_image_awards_winner
        )

    # TODO: Remove `and self.is_innopac_id_8_digits`
    @property
    def is_catalogue_api(self):
        return (
            not self.image_library_dept_is_Public_programmes
            and self.is_for_public_access
            and (self._get("image_innopac_id") is None or self.is_innopac_id_8_digits)
        )


class Decision(enum.Enum):
    cold_store = "cold_store"
    tandem_vault = "tandem_vault"
    catalogue_api = "catalogue_api"
    none = "none"


class InvalidCollectionException(Exception):
    pass


def _get_decisions_from_id_exceptions(exceptions, image_data):
    for exception in exceptions:
        if exception.pop("miro_id").strip() == image_data["image_no_calc"]:
            return [
                getattr(Decision, key)
                for key, value in exception.items()
                if value is not "" and not value.strip().lower() == "false"
            ]

    # There are "holding images" in MIRO, which are thumbnails put into
    # duplicate image records for some explicit AIDS posters.  All the
    # posters are available, so we delete these records.  They all have
    # image numbers in the L sequence ending with "FX", e.g. "L0052198FX".
    if re.match(r"^L\d+FX$", image_data["image_no_calc"]):
        return [Decision.cold_store]


def _get_decisions_from_contrib_exceptions(collection, exceptions, image_data):
    collections = exceptions.fieldnames
    collection = collection.split("-")[-1]

    if collection in collections:
        contrib_codes = [row[collection] for row in exceptions]
        image_source_code = image_data["image_source_code"]

        if image_source_code in contrib_codes:
            return [Decision.catalogue_api]
        else:
            return [Decision.cold_store]

    else:
        return None


def _get_decisions_from_rules(collection, image_data):
    decisions = []
    r = Rules(collection, image_data)
    if not r.is_collection("D", "F", "L", "V", "M", "FP", "AS", "S"):
        raise InvalidCollectionException(
            {"collection": collection, "image_data": image_data}
        )

    if r.is_cold_store:
        decisions = [Decision.cold_store]

    if not r.is_cold_store and r.is_tandem_vault:
        decisions.append(Decision.tandem_vault)

    if not r.is_cold_store and r.is_catalogue_api:
        decisions.append(Decision.catalogue_api)

    if not r.is_cold_store and not r.is_catalogue_api and not r.is_tandem_vault:
        decisions.append(Decision.none)

    print(f"_get_decisions_from_rules = {decisions}")

    return decisions


def _assess_rules(rule_list):
    for rule in rule_list:
        decisions = rule()

        print(f"_assess_rules = {decisions}")

        if decisions is not None:
            return decisions

    return []


def sort_image(collection, image_data, id_exceptions, contrib_exceptions):
    print(f"collection = {collection}")
    print(f"image_data = {image_data}")

    decisions = _assess_rules(
        [
            lambda: _get_decisions_from_id_exceptions(id_exceptions, image_data),
            lambda: _get_decisions_from_contrib_exceptions(
                collection, contrib_exceptions, image_data
            ),
            lambda: _get_decisions_from_rules(collection, image_data),
        ]
    )

    # Wellcome Images Awards winners *always* go to Tandem Vault, in addition
    # to any other rules we might have applied.
    if Decision.tandem_vault not in decisions:
        r = Rules(collection=collection, image_data=image_data)
        if r.is_a_wellcome_image_awards_winner:
            decisions.append(Decision.tandem_vault)

    return decisions