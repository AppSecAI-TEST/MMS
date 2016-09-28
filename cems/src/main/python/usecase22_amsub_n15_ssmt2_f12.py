from workflow import Workflow

w = Workflow('usecase22_amsub_n15_ssmt2_f12', 7, '/group_workspaces/cems2/fiduceo/Software/mms/config')
w.add_primary_sensor('amsub-n15', '1999-01-01', '2002-07-30', 'v1.0')
w.add_secondary_sensor('ssmt2-f12', '1999-01-01', '2002-07-30', 'v01')

w.set_usecase_config('usecase-22.xml')

w.run_matchup(hosts=[('localhost', 96)])