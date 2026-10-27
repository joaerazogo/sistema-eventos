######################################
##        Variables Globales        ##
######################################

env  = "dev"
tags = { project = "ticketing", env = "dev" }

######################################
##          Variables SQS           ##
######################################

visibility_timeout = 30
message_retention  = 86400
max_receive_count  = 3
