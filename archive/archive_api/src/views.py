# -*- encoding: utf-8


from flask import jsonify, make_response, request
from flask_restplus import Resource
from werkzeug.exceptions import BadRequest as BadRequestError
from werkzeug.exceptions import NotFound as NotFoundError

from archive_api import app, api, logger
from ingests import send_new_ingest_request
import models
from progress_manager import ProgressNotFoundError
import validators


progress_manager = app.config["PROGRESS_MANAGER"]

# api.namespaces.clear()
ns_ingests = api.namespace("ingests", description="Ingest requests")
# ns_bags = api.namespace("bags", description="Bag requests")

# ns_ingests.add_model(name="IngestRequest", definition=models.IngestRequest)
# ns_ingests.add_model(name="IngestType", definition=models.IngestType)

api.add_model(name="Error", definition=models.Error)


# @ns_ingests.route("")
# @ns_ingests.doc(description="Request the ingest of a BagIt resource.")
# @ns_ingests.param(
#     "payload",
#     "The ingest request specifying the uploadUrl where the BagIt resource can be found",
#     _in="body",
# )
# class IngestCollection(Resource):
#     @ns_ingests.expect(models.IngestRequest, validate=True)
#     @ns_ingests.response(202, "Ingest created")
#     @ns_ingests.response(400, "Bad request", models.Error)
#     def post(self):
#         """Create a request to ingest a BagIt resource"""
#         upload_url = request.json["uploadUrl"]
#         callback_url = request.json.get("callbackUrl")
#         self.validate_urls(callback_url, upload_url)
#
#         ingest_request_id = progress_manager.create_request(
#             upload_url=upload_url, callback_url=callback_url
#         )
#         logger.debug("ingest_request_id=%r", ingest_request_id)
#
#         ingest_request_id = send_new_ingest_request(
#             sns_client=app.config["SNS_CLIENT"],
#             topic_arn=app.config["SNS_TOPIC_ARN"],
#             ingest_request_id=ingest_request_id,
#             upload_url=upload_url,
#             callback_url=callback_url,
#         )
#
#         # Construct the URL where the user will be able to get the status
#         # of their ingest request.
#         location = api.url_for(IngestResource, id=ingest_request_id)
#
#         # Now we set the Location response header.  There's no way to do this
#         # without constructing our own Response object, so that's what we do
#         # here.  See https://stackoverflow.com/q/25860304/1558022
#         resp = make_response("", 202)
#         resp.headers["Location"] = location
#         return resp
#
#     def validate_urls(self, callback_url, upload_url):
#         try:
#             validators.validate_upload_url(upload_url)
#         except ValueError as error:
#             raise BadRequestError(f"Invalid uploadUrl:{upload_url!r}, {error}")
#
#         if callback_url is not None:
#             try:
#                 validators.validate_callback_url(callback_url)
#             except ValueError as error:
#                 raise BadRequestError(f"Invalid callbackUrl:{callback_url!r}, {error}")
#
#
# @ns_ingests.route("/<string:id>")
# @ns_ingests.param("id", "The ingest request identifier")
# class IngestResource(Resource):
#     @ns_ingests.doc(
#         description="The ingest request id is returned in the Location header from a POSTed ingest request"
#     )
#     @ns_ingests.response(200, "Ingest found")
#     @ns_ingests.response(404, "Ingest not found", models.Error)
#     def get(self, id):
#         """Get the current status of an ingest request"""
#         try:
#             result = progress_manager.lookup_progress(id=id)
#             return result
#         except ProgressNotFoundError as error:
#             raise NotFoundError(f"Invalid id: No ingest found for id={id!r}")


@app.route("/storage/v1/healthcheck")
def route_report_healthcheck_status():
    return {"status": "OK"}


@app.errorhandler(Exception)
@api.errorhandler(Exception)
@api.marshal_with(models.Error, skip_none=True)
def default_error_handler(error):
    error_response = {
        "httpStatus": getattr(error, "code", 500),
        "label": getattr(error, "name", "Internal Server Error"),
    }
    logger.warn(error)
    if error_response["httpStatus"] != 500:
        if hasattr(error, "data"):
            error_response["description"] = ", ".join(
                error.data.get("errors", {}).values()
            )
        else:
            error_response["description"] = getattr(error, "description", str(error))
    return error_response, error_response["httpStatus"]
