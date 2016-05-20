from workflow import Workflow

w = Workflow('usecase17_mhs_n18_amsub_n15', 7, '/group_workspaces/cems2/fiduceo/Software/mms/config')
w.add_primary_sensor('mhs-n18', '2005-05-25', '2016-03-04', 'v1.0')
w.add_secondary_sensor('amsub-n15', '2005-05-25', '2016-03-04', 'v1.0')

w.set_usecase_config('usecase-17.xml')

w.run_matchup(hosts=[('localhost', 72)])