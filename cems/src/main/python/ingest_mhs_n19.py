from workflow import Workflow

w = Workflow('ingest_mhs_n19', 7, '/group_workspaces/cems2/fiduceo/Software/mms/config')
w.add_primary_sensor('mhs-n19', '2009-04-01', '2016-03-04', 'v1.0')

w.run_ingestion(hosts=[('localhost', 24)])