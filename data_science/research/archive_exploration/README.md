# Archive Exploration
The Wellcome archive sits in a collections management system called CALM, which follows a rough set of standards and guidelines for storing archival records called [ISAD(G)](https://en.wikipedia.org/wiki/ISAD(G)). The archive is comprised of _collections_, each of which has a hierarcal set of series, sections, subjects, items and pieces sitting underneath it.  
One day, the platform team will need to ingest, process and serve up the archive data on the [catalogue API](https://developers.wellcomecollection.org/) and [`/works`](https://wellcomecollection.org/works). 

In the following notebooks I'll explore the CALM data and try to make as much sense of it as I can programatically. Currently they show the process I follow when exploring a new, semi-structured dataset, and the kind of straightforward, serendipitous results which can be extracted without investing too much time or effort.