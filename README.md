<div align="center">
<h1>CruzAPIM-Next</h1>
</div>
 <br>
<div align="center">
<h3>CruzAPIM SonarQube Status</h3>
<img src="https://sonarqube.direa.synology.me/api/project_badges/measure?project=kr.co.direa%3Apump&metric=alert_status&token=4805792dc39db137bbababbfeb0a58df74faadf6">
<img src="https://sonarqube.direa.synology.me/api/project_badges/measure?project=kr.co.direa%3Apump&metric=reliability_rating&token=4805792dc39db137bbababbfeb0a58df74faadf6">
<img src="https://sonarqube.direa.synology.me/api/project_badges/measure?project=kr.co.direa%3Apump&metric=security_rating&token=4805792dc39db137bbababbfeb0a58df74faadf6">
<img src="https://sonarqube.direa.synology.me/api/project_badges/measure?project=kr.co.direa%3Apump&metric=vulnerabilities&token=4805792dc39db137bbababbfeb0a58df74faadf6">
<img src="https://sonarqube.direa.synology.me/api/project_badges/measure?project=kr.co.direa%3Apump&metric=bugs&token=4805792dc39db137bbababbfeb0a58df74faadf6">
<img src="https://sonarqube.direa.synology.me/api/project_badges/measure?project=kr.co.direa%3Apump&metric=code_smells&token=4805792dc39db137bbababbfeb0a58df74faadf6">
<img src="https://sonarqube.direa.synology.me/api/project_badges/measure?project=kr.co.direa%3Apump&metric=ncloc&token=4805792dc39db137bbababbfeb0a58df74faadf6">
<img src="https://sonarqube.direa.synology.me/api/project_badges/measure?project=kr.co.direa%3Apump&metric=coverage&token=4805792dc39db137bbababbfeb0a58df74faadf6">
</div>

## Profiles
1. local 실행 시, profiles local 설정
2. 서버에 패치할 파일 빌드할 때는 profiles dev 설정
    ```shell
    clean package -DskipTests -P dev
    ````
## Engine 로컬 실행 시 
VM option 추가 
```shell
--add-opens=java.base/java.net=ALL-UNNAMED
```

## 로컬 테스트를 위한 PreRequisite
### Oracle19C (Docker)   
1. 이미지 다운로드   
    ```shell
    docker pull doctorkirk/oracle-19c
    ```
2. 이미지 실행   
   - 컨테이너 재기동시 데이터 유실을 방지하기 위해 로컬 볼륨 마운트 필수
   ```shell
   docker run --name oracle19c -d -p 1521:1521 -e ORACLE_SID=ORCL \
   -e ORACLE_PWD=direa -e ORACLE_CHARACTERSET=KO16MSWIN949 -v \
   D:\DireaDevEnv\Oracle\oradata:/opt/oracle/oradata doctorkirk/oracle-19c
   ```   
       
3. 계정생성 및 권한부여   
   - 컨테이너에 접속
   ```shell
   docker ps 
   ...
   docker exec -it {CONTAINER ID} bash
   ```
   - 계정 생성
   ```shell
   sqlplus '/as sysdba'
   create user direa identified by direa;
   grant connect, resource to direa;
   ALTER USER direa DEFAULT TABLESPACE USERS QUOTA UNLIMITED ON USERS;
   ```

4. 접속 방법   
   ```
   sid: ORCL   
   port: 1521   
   ip: localhost   
   ```

### rabbitMQ (Docker)   
1. 이미지 다운로드
    ```shell
    docker run -d --name rabbitmq -p 15672:15672 -p 5672:5672 \
    -p 15671:15671 -p 5671:5671 -p 4369:4369 -e RABBITMQ_DEFAULT_USER=direa \
    -e RABBITMQ_DEFAULT_PASS=direa -v D:\DireaDevEnv\RabbitMQ\data:/var/lib/rabbitmq/ \
    -v C:\DireaDevEnv\RabbitMQ\logs:/var/log/rabbitmq/ rabbitmq:management
    ```

2. 계정
    ```
    id: direa
    pw: direa
    ```
   

## DB - 최초 실행 시 
spring.sql.init.mode=always => 테이블 생성, 데이터 insert
   

----------------------------

### Maven Build 
- apim-common -> Gateway build
```shell
 mvn clean install -pl apim-common -amd -DskipTests
```


### Docker Build  
docker build -t {이름} .

--------------------------

## 설치 파일(tar) 생성 시
- data, logs 파일 제외하고 tar 생성
```shell
tar -cvf cruzapim.tar --exclude cruzapim/data --exclude cruzapim/logs cruzapim/
```
- <.sql> 파일은 UTF-8 인코딩 형식으로 공유될 수 있도록 형식 확인.
