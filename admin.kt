Microsoft Windows [Version 10.0.17763.8389]
(c) 2018 Microsoft Corporation. All rights reserved.

C:\Users\Administrator>cd C:\TunerToolsGP_Fresh

C:\TunerToolsGP_Fresh>py -m pip install -r requirements.txt
Collecting google-api-python-client<3,>=2.140.0 (from -r requirements.txt (line 1))
  Downloading google_api_python_client-2.201.0-py3-none-any.whl.metadata (6.8 kB)
Collecting google-auth<3,>=2.25.0 (from -r requirements.txt (line 2))
  Downloading google_auth-2.62.0-py3-none-any.whl.metadata (6.0 kB)
Collecting google-auth-oauthlib<2,>=1.2.0 (from -r requirements.txt (line 3))
  Downloading google_auth_oauthlib-1.5.0-py3-none-any.whl.metadata (2.6 kB)
Collecting httplib2<1.0.0,>=0.19.0 (from google-api-python-client<3,>=2.140.0->-r requirements.txt (line 1))
  Downloading httplib2-0.32.0-py3-none-any.whl.metadata (2.2 kB)
Collecting google-auth-httplib2<1.0.0,>=0.2.0 (from google-api-python-client<3,>=2.140.0->-r requirements.txt (line 1))
  Downloading google_auth_httplib2-0.4.4-py3-none-any.whl.metadata (3.0 kB)
Collecting google-api-core!=2.0.*,!=2.1.*,!=2.2.*,!=2.3.0,<3.0.0,>=1.31.5 (from google-api-python-client<3,>=2.140.0->-r requirements.txt (line 1))
  Downloading google_api_core-2.43.0-py3-none-any.whl.metadata (3.2 kB)
Collecting uritemplate<5,>=3.0.1 (from google-api-python-client<3,>=2.140.0->-r requirements.txt (line 1))
  Downloading uritemplate-4.2.0-py3-none-any.whl.metadata (2.6 kB)
Collecting pyasn1-modules>=0.2.1 (from google-auth<3,>=2.25.0->-r requirements.txt (line 2))
  Downloading pyasn1_modules-0.4.2-py3-none-any.whl.metadata (3.5 kB)
Collecting cryptography>=38.0.3 (from google-auth<3,>=2.25.0->-r requirements.txt (line 2))
  Downloading cryptography-50.0.2-cp311-abi3-win_amd64.whl.metadata (4.4 kB)
Collecting requests-oauthlib>=0.7.0 (from google-auth-oauthlib<2,>=1.2.0->-r requirements.txt (line 3))
  Downloading requests_oauthlib-2.0.0-py2.py3-none-any.whl.metadata (11 kB)
Collecting cffi>=2.0.0 (from cryptography>=38.0.3->google-auth<3,>=2.25.0->-r requirements.txt (line 2))
  Downloading cffi-2.1.1-cp311-cp311-win_amd64.whl.metadata (2.6 kB)
Collecting googleapis-common-protos<2.0.0,>=1.69.2 (from google-api-core!=2.0.*,!=2.1.*,!=2.2.*,!=2.3.0,<3.0.0,>=1.31.5->google-api-python-client<3,>=2.140.0->-r requirements.txt (line 1))
  Downloading googleapis_common_protos-1.76.0-py3-none-any.whl.metadata (8.5 kB)
Collecting protobuf<8.0.0,>=6.33.5 (from google-api-core!=2.0.*,!=2.1.*,!=2.2.*,!=2.3.0,<3.0.0,>=1.31.5->google-api-python-client<3,>=2.140.0->-r requirements.txt (line 1))
  Downloading protobuf-7.36.2-cp310-abi3-win_amd64.whl.metadata (595 bytes)
Collecting proto-plus<2.0.0,>=1.26.1 (from google-api-core!=2.0.*,!=2.1.*,!=2.2.*,!=2.3.0,<3.0.0,>=1.31.5->google-api-python-client<3,>=2.140.0->-r requirements.txt (line 1))
  Downloading proto_plus-1.29.0-py3-none-any.whl.metadata (2.2 kB)
Collecting requests<3.0.0,>=2.33.0 (from google-api-core!=2.0.*,!=2.1.*,!=2.2.*,!=2.3.0,<3.0.0,>=1.31.5->google-api-python-client<3,>=2.140.0->-r requirements.txt (line 1))
  Downloading requests-2.34.2-py3-none-any.whl.metadata (4.8 kB)
Collecting opentelemetry-api<2.0.0,>=1.44.0 (from google-api-core!=2.0.*,!=2.1.*,!=2.2.*,!=2.3.0,<3.0.0,>=1.31.5->google-api-python-client<3,>=2.140.0->-r requirements.txt (line 1))
  Downloading opentelemetry_api-1.45.1-py3-none-any.whl.metadata (1.4 kB)
Collecting pyparsing<4,>=3.1 (from httplib2<1.0.0,>=0.19.0->google-api-python-client<3,>=2.140.0->-r requirements.txt (line 1))
  Downloading pyparsing-3.3.3-py3-none-any.whl.metadata (5.9 kB)
Collecting pyasn1<0.7.0,>=0.6.1 (from pyasn1-modules>=0.2.1->google-auth<3,>=2.25.0->-r requirements.txt (line 2))
  Downloading pyasn1-0.6.5-py3-none-any.whl.metadata (8.4 kB)
Collecting oauthlib>=3.0.0 (from requests-oauthlib>=0.7.0->google-auth-oauthlib<2,>=1.2.0->-r requirements.txt (line 3))
  Downloading oauthlib-4.0.0-py3-none-any.whl.metadata (8.6 kB)
Collecting pycparser (from cffi>=2.0.0->cryptography>=38.0.3->google-auth<3,>=2.25.0->-r requirements.txt (line 2))
  Downloading pycparser-3.12-py3-none-any.whl.metadata (9.7 kB)
Requirement already satisfied: typing-extensions>=4.5.0 in c:\program files\python311\lib\site-packages (from opentelemetry-api<2.0.0,>=1.44.0->google-api-core!=2.0.*,!=2.1.*,!=2.2.*,!=2.3.0,<3.0.0,>=1.31.5->google-api-python-client<3,>=2.140.0->-r requirements.txt (line 1)) (4.16.0)
Collecting charset_normalizer<4,>=2 (from requests<3.0.0,>=2.33.0->google-api-core!=2.0.*,!=2.1.*,!=2.2.*,!=2.3.0,<3.0.0,>=1.31.5->google-api-python-client<3,>=2.140.0->-r requirements.txt (line 1))
  Downloading charset_normalizer-3.5.2-cp311-cp311-win_amd64.whl.metadata (47 kB)
     ---------------------------------------- 47.3/47.3 kB 597.5 kB/s eta 0:00:00
Collecting idna<4,>=2.5 (from requests<3.0.0,>=2.33.0->google-api-core!=2.0.*,!=2.1.*,!=2.2.*,!=2.3.0,<3.0.0,>=1.31.5->google-api-python-client<3,>=2.140.0->-r requirements.txt (line 1))
  Downloading idna-3.20-py3-none-any.whl.metadata (7.2 kB)
Collecting urllib3<3,>=1.26 (from requests<3.0.0,>=2.33.0->google-api-core!=2.0.*,!=2.1.*,!=2.2.*,!=2.3.0,<3.0.0,>=1.31.5->google-api-python-client<3,>=2.140.0->-r requirements.txt (line 1))
  Downloading urllib3-2.8.0-py3-none-any.whl.metadata (7.4 kB)
Collecting certifi>=2023.5.7 (from requests<3.0.0,>=2.33.0->google-api-core!=2.0.*,!=2.1.*,!=2.2.*,!=2.3.0,<3.0.0,>=1.31.5->google-api-python-client<3,>=2.140.0->-r requirements.txt (line 1))
  Downloading certifi-2026.10.23-py3-none-any.whl.metadata (2.5 kB)
Downloading google_api_python_client-2.201.0-py3-none-any.whl (16.8 MB)
   ---------------------------------------- 16.8/16.8 MB 11.1 MB/s eta 0:00:00
Downloading google_auth-2.62.0-py3-none-any.whl (267 kB)
   ---------------------------------------- 267.9/267.9 kB 16.1 MB/s eta 0:00:00
Downloading google_auth_oauthlib-1.5.0-py3-none-any.whl (20 kB)
Downloading cryptography-50.0.2-cp311-abi3-win_amd64.whl (3.8 MB)
   ---------------------------------------- 3.8/3.8 MB 11.1 MB/s eta 0:00:00
Downloading google_api_core-2.43.0-py3-none-any.whl (223 kB)
   ---------------------------------------- 223.5/223.5 kB 13.3 MB/s eta 0:00:00
Downloading google_auth_httplib2-0.4.4-py3-none-any.whl (9.5 kB)
Downloading httplib2-0.32.0-py3-none-any.whl (93 kB)
   ---------------------------------------- 93.1/93.1 kB 5.2 MB/s eta 0:00:00
Downloading pyasn1_modules-0.4.2-py3-none-any.whl (181 kB)
   ---------------------------------------- 181.3/181.3 kB 10.7 MB/s eta 0:00:00
Downloading requests_oauthlib-2.0.0-py2.py3-none-any.whl (24 kB)
Downloading uritemplate-4.2.0-py3-none-any.whl (11 kB)
Downloading cffi-2.1.1-cp311-cp311-win_amd64.whl (185 kB)
   ---------------------------------------- 185.1/185.1 kB 10.9 MB/s eta 0:00:00
Downloading googleapis_common_protos-1.76.0-py3-none-any.whl (307 kB)
   ---------------------------------------- 307.7/307.7 kB 19.8 MB/s eta 0:00:00
Downloading oauthlib-4.0.0-py3-none-any.whl (159 kB)
   ---------------------------------------- 159.7/159.7 kB 9.3 MB/s eta 0:00:00
Downloading opentelemetry_api-1.45.1-py3-none-any.whl (60 kB)
   ---------------------------------------- 60.3/60.3 kB ? eta 0:00:00
Downloading proto_plus-1.29.0-py3-none-any.whl (50 kB)
   ---------------------------------------- 50.8/50.8 kB 2.5 MB/s eta 0:00:00
Downloading protobuf-7.36.2-cp310-abi3-win_amd64.whl (456 kB)
   ---------------------------------------- 456.5/456.5 kB 14.4 MB/s eta 0:00:00
Downloading pyasn1-0.6.5-py3-none-any.whl (84 kB)
   ---------------------------------------- 84.8/84.8 kB 4.7 MB/s eta 0:00:00
Downloading pyparsing-3.3.3-py3-none-any.whl (126 kB)
   ---------------------------------------- 126.4/126.4 kB 7.7 MB/s eta 0:00:00
Downloading requests-2.34.2-py3-none-any.whl (73 kB)
   ---------------------------------------- 73.1/73.1 kB ? eta 0:00:00
Downloading certifi-2026.10.23-py3-none-any.whl (135 kB)
   ---------------------------------------- 135.1/135.1 kB ? eta 0:00:00
Downloading charset_normalizer-3.5.2-cp311-cp311-win_amd64.whl (214 kB)
   ---------------------------------------- 214.9/214.9 kB 13.6 MB/s eta 0:00:00
Downloading idna-3.20-py3-none-any.whl (69 kB)
   ---------------------------------------- 69.6/69.6 kB ? eta 0:00:00
Downloading urllib3-2.8.0-py3-none-any.whl (135 kB)
   ---------------------------------------- 135.7/135.7 kB 7.8 MB/s eta 0:00:00
Downloading pycparser-3.12-py3-none-any.whl (62 kB)
   ---------------------------------------- 62.6/62.6 kB ? eta 0:00:00
Installing collected packages: urllib3, uritemplate, pyparsing, pycparser, pyasn1, protobuf, opentelemetry-api, oauthlib, idna, charset_normalizer, certifi, requests, pyasn1-modules, proto-plus, httplib2, googleapis-common-protos, cffi, requests-oauthlib, cryptography, google-auth, google-auth-oauthlib, google-auth-httplib2, google-api-core, google-api-python-client
Successfully installed certifi-2026.10.23 cffi-2.1.1 charset_normalizer-3.5.2 cryptography-50.0.2 google-api-core-2.43.0 google-api-python-client-2.201.0 google-auth-2.62.0 google-auth-httplib2-0.4.4 google-auth-oauthlib-1.5.0 googleapis-common-protos-1.76.0 httplib2-0.32.0 idna-3.20 oauthlib-4.0.0 opentelemetry-api-1.45.1 proto-plus-1.29.0 protobuf-7.36.2 pyasn1-0.6.5 pyasn1-modules-0.4.2 pycparser-3.12 pyparsing-3.3.3 requests-2.34.2 requests-oauthlib-2.0.0 uritemplate-4.2.0 urllib3-2.8.0

[notice] A new release of pip is available: 24.0 -> 26.2.2
[notice] To update, run: python.exe -m pip install --upgrade pip

C:\TunerToolsGP_Fresh>
