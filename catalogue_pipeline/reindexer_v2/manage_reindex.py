#!/usr/bin/env python
# -*- encoding: utf-8
"""
Create/update reindex shards in the reindex shard tracker table.

Usage: manage_reindex.py update-shards --prefix=<PREFIX> --count=<COUNT> [--desired_version=<VERSION>] [--table=<TABLE>]
       manage_reindex.py -h | --help

Actions:
  update-shards         Create or update shards in the reindex tracker table.

Options:
  --prefix=<PREFIX>     Name of the reindex shard prefix, e.g. sierra, miro
  --count=<COUNT>       How many shards to create in the table
  --desired_version=<VERSION>
                        Desired version of all rows in the reindexed table
  --table=<TABLE>       Name of the reindex shard tracker DynamoDB table
  -h --help             Print this help message

"""

import boto3
import docopt
import tqdm


# Implementation note: this code may fail if you hit the write limit on
# DynamoDB, and interrupt midway through updating a prefix.
#
#  1. Decorating this function with the tenacity library
#     (http://tenacity.readthedocs.io/en/latest/) should make it more likely
#     to succeed, as it will retry in case of error.
#
#  2. The whole process is idempotent, so it's enough to bump the capacity
#     and re-run the prefix.
#
# FWIW, I was able to create >5000 shards with WriteCapacity=1 and autoscaling
# on a test table, so this may be a theoretical concern!

def _update_shard(client, table_name, shard):
    client.update_item(
        TableName=table_name,
        Key={'shardId': {'S': shard['shardId']}},

        # We want to update the desiredVersion of every row in the table, and
        # set the currentVersion to 1, but *only* if there isn't already an
        # existing value of currentVersion in the table.
        UpdateExpression=(
            'SET desiredVersion = :desiredVersion, '
            'currentVersion = if_not_exists(currentVersion, :initialCurrentVersion)'
        ),
        ExpressionAttributeValues={
            ':desiredVersion': {'N': str(shard['desiredVersion'])},
            ':initialCurrentVersion': {'N': '1'},
        }
    )


def create_shards(prefix, desired_version, count, table_name):
    """Create new shards in the table."""
    new_shards = [
        {'shardId': f'{prefix}/{i}', 'desiredVersion': desired_version}
        for i in range(count)
    ]

    client = boto3.client('dynamodb')

    # Implementation note: this is potentially quite slow, as we make a new
    # UPDATE request for every shard we want to write to DynamoDB.  The
    # reason is that if we just PUT an item, we delete whatever's already
    # there -- potentially including the currentVersion.
    #
    # We could speed this up slightly by:
    #
    #   1.  GET the current row (if any)
    #   2.  Construct the new row, and try to PUT that, using a conditional
    #       update to check nobody has updated :currentVersion in the meantime
    #   3.  Repeat 1-2 until we get an update that succeeds
    #
    # That would be significantly more complicated to implement, so for now
    # we choose simplicity over speed.
    #
    # If you're finding this script to be too slow, this is where to start.
    for shard in tqdm.tqdm(new_shards):
        _update_shard(client=client, table_name=table_name, shard=shard)


if __name__ == '__main__':
    args = docopt.docopt(__doc__)

    default_table_name = 'ReindexShardTracker'

    if args['update-shards']:
        prefix = args['--prefix']
        count = int(args['--count'])
        desired_version = int(args['--desired_version'] or '1')
        table_name = args['--table'] or default_table_name

        create_shards(
            prefix=prefix,
            desired_version=desired_version,
            count=count,
            table_name=table_name
        )