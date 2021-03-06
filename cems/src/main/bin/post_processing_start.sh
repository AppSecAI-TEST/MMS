#!/bin/bash

. ${MMS_HOME}/bin/${MMS_ENV_NAME}

input_dir=$1
start_date=$2
end_date=$3
job_config=$4
config_dir=$5

task="post_processing"
jobname="${task}-${job_config}-${start_date}-${end_date}"
echo ${jobname}

command="${task}_run.sh ${input_dir} ${start_date} ${end_date} ${job_config} ${config_dir}"

echo "`date -u +%Y%m%d-%H%M%S` submitting job '${jobname}'"

read_task_jobs ${jobname}

if [ -z ${jobs} ]; then
    submit_job ${jobname} ${command}
fi

wait_for_task_jobs_completion ${jobname}