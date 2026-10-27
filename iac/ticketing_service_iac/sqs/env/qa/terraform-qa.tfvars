######################################
##        Variables Globales        ##
######################################

env  = "qa"
tags = { project = "ticketing", env = "qa" }

######################################
##          Variables SQS           ##
######################################

visibility_timeout = 30
message_retention  = 86400
max_receive_count  = 3
