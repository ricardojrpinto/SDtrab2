
package fileclient.ws;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the fileclient.ws package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _GetFileInfo_QNAME = new QName("http://fileserver/", "getFileInfo");
    private final static QName _DownloadFileResponse_QNAME = new QName("http://fileserver/", "downloadFileResponse");
    private final static QName _UserPermissionException_QNAME = new QName("http://fileserver/", "UserPermissionException");
    private final static QName _RmdirResponse_QNAME = new QName("http://fileserver/", "rmdirResponse");
    private final static QName _Rm_QNAME = new QName("http://fileserver/", "rm");
    private final static QName _Rmdir_QNAME = new QName("http://fileserver/", "rmdir");
    private final static QName _Mkdir_QNAME = new QName("http://fileserver/", "mkdir");
    private final static QName _MkdirResponse_QNAME = new QName("http://fileserver/", "mkdirResponse");
    private final static QName _GetServerInfo_QNAME = new QName("http://fileserver/", "getServerInfo");
    private final static QName _InfoNotFoundException_QNAME = new QName("http://fileserver/", "InfoNotFoundException");
    private final static QName _AddPermissionResponse_QNAME = new QName("http://fileserver/", "addPermissionResponse");
    private final static QName _DownloadFile_QNAME = new QName("http://fileserver/", "downloadFile");
    private final static QName _RmResponse_QNAME = new QName("http://fileserver/", "rmResponse");
    private final static QName _RemovePermissionResponse_QNAME = new QName("http://fileserver/", "removePermissionResponse");
    private final static QName _AddPermission_QNAME = new QName("http://fileserver/", "addPermission");
    private final static QName _RemovePermission_QNAME = new QName("http://fileserver/", "removePermission");
    private final static QName _UploadFile_QNAME = new QName("http://fileserver/", "uploadFile");
    private final static QName _UploadFileResponse_QNAME = new QName("http://fileserver/", "uploadFileResponse");
    private final static QName _GetServerInfoResponse_QNAME = new QName("http://fileserver/", "getServerInfoResponse");
    private final static QName _Dir_QNAME = new QName("http://fileserver/", "dir");
    private final static QName _GetFileInfoResponse_QNAME = new QName("http://fileserver/", "getFileInfoResponse");
    private final static QName _DirResponse_QNAME = new QName("http://fileserver/", "dirResponse");
    private final static QName _UploadFileArg1_QNAME = new QName("", "arg1");
    private final static QName _DownloadFileResponseReturn_QNAME = new QName("", "return");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: fileclient.ws
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link UserPermissionException }
     * 
     */
    public UserPermissionException createUserPermissionException() {
        return new UserPermissionException();
    }

    /**
     * Create an instance of {@link GetFileInfo }
     * 
     */
    public GetFileInfo createGetFileInfo() {
        return new GetFileInfo();
    }

    /**
     * Create an instance of {@link DownloadFileResponse }
     * 
     */
    public DownloadFileResponse createDownloadFileResponse() {
        return new DownloadFileResponse();
    }

    /**
     * Create an instance of {@link Rm }
     * 
     */
    public Rm createRm() {
        return new Rm();
    }

    /**
     * Create an instance of {@link RmdirResponse }
     * 
     */
    public RmdirResponse createRmdirResponse() {
        return new RmdirResponse();
    }

    /**
     * Create an instance of {@link MkdirResponse }
     * 
     */
    public MkdirResponse createMkdirResponse() {
        return new MkdirResponse();
    }

    /**
     * Create an instance of {@link GetServerInfo }
     * 
     */
    public GetServerInfo createGetServerInfo() {
        return new GetServerInfo();
    }

    /**
     * Create an instance of {@link Rmdir }
     * 
     */
    public Rmdir createRmdir() {
        return new Rmdir();
    }

    /**
     * Create an instance of {@link Mkdir }
     * 
     */
    public Mkdir createMkdir() {
        return new Mkdir();
    }

    /**
     * Create an instance of {@link AddPermissionResponse }
     * 
     */
    public AddPermissionResponse createAddPermissionResponse() {
        return new AddPermissionResponse();
    }

    /**
     * Create an instance of {@link DownloadFile }
     * 
     */
    public DownloadFile createDownloadFile() {
        return new DownloadFile();
    }

    /**
     * Create an instance of {@link InfoNotFoundException }
     * 
     */
    public InfoNotFoundException createInfoNotFoundException() {
        return new InfoNotFoundException();
    }

    /**
     * Create an instance of {@link RemovePermissionResponse }
     * 
     */
    public RemovePermissionResponse createRemovePermissionResponse() {
        return new RemovePermissionResponse();
    }

    /**
     * Create an instance of {@link UploadFileResponse }
     * 
     */
    public UploadFileResponse createUploadFileResponse() {
        return new UploadFileResponse();
    }

    /**
     * Create an instance of {@link UploadFile }
     * 
     */
    public UploadFile createUploadFile() {
        return new UploadFile();
    }

    /**
     * Create an instance of {@link RemovePermission }
     * 
     */
    public RemovePermission createRemovePermission() {
        return new RemovePermission();
    }

    /**
     * Create an instance of {@link AddPermission }
     * 
     */
    public AddPermission createAddPermission() {
        return new AddPermission();
    }

    /**
     * Create an instance of {@link RmResponse }
     * 
     */
    public RmResponse createRmResponse() {
        return new RmResponse();
    }

    /**
     * Create an instance of {@link Dir }
     * 
     */
    public Dir createDir() {
        return new Dir();
    }

    /**
     * Create an instance of {@link GetServerInfoResponse }
     * 
     */
    public GetServerInfoResponse createGetServerInfoResponse() {
        return new GetServerInfoResponse();
    }

    /**
     * Create an instance of {@link DirResponse }
     * 
     */
    public DirResponse createDirResponse() {
        return new DirResponse();
    }

    /**
     * Create an instance of {@link GetFileInfoResponse }
     * 
     */
    public GetFileInfoResponse createGetFileInfoResponse() {
        return new GetFileInfoResponse();
    }

    /**
     * Create an instance of {@link WsFileServerInfo }
     * 
     */
    public WsFileServerInfo createWsFileServerInfo() {
        return new WsFileServerInfo();
    }

    /**
     * Create an instance of {@link FileInfo }
     * 
     */
    public FileInfo createFileInfo() {
        return new FileInfo();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetFileInfo }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://fileserver/", name = "getFileInfo")
    public JAXBElement<GetFileInfo> createGetFileInfo(GetFileInfo value) {
        return new JAXBElement<GetFileInfo>(_GetFileInfo_QNAME, GetFileInfo.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DownloadFileResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://fileserver/", name = "downloadFileResponse")
    public JAXBElement<DownloadFileResponse> createDownloadFileResponse(DownloadFileResponse value) {
        return new JAXBElement<DownloadFileResponse>(_DownloadFileResponse_QNAME, DownloadFileResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UserPermissionException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://fileserver/", name = "UserPermissionException")
    public JAXBElement<UserPermissionException> createUserPermissionException(UserPermissionException value) {
        return new JAXBElement<UserPermissionException>(_UserPermissionException_QNAME, UserPermissionException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RmdirResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://fileserver/", name = "rmdirResponse")
    public JAXBElement<RmdirResponse> createRmdirResponse(RmdirResponse value) {
        return new JAXBElement<RmdirResponse>(_RmdirResponse_QNAME, RmdirResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Rm }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://fileserver/", name = "rm")
    public JAXBElement<Rm> createRm(Rm value) {
        return new JAXBElement<Rm>(_Rm_QNAME, Rm.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Rmdir }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://fileserver/", name = "rmdir")
    public JAXBElement<Rmdir> createRmdir(Rmdir value) {
        return new JAXBElement<Rmdir>(_Rmdir_QNAME, Rmdir.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Mkdir }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://fileserver/", name = "mkdir")
    public JAXBElement<Mkdir> createMkdir(Mkdir value) {
        return new JAXBElement<Mkdir>(_Mkdir_QNAME, Mkdir.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link MkdirResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://fileserver/", name = "mkdirResponse")
    public JAXBElement<MkdirResponse> createMkdirResponse(MkdirResponse value) {
        return new JAXBElement<MkdirResponse>(_MkdirResponse_QNAME, MkdirResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetServerInfo }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://fileserver/", name = "getServerInfo")
    public JAXBElement<GetServerInfo> createGetServerInfo(GetServerInfo value) {
        return new JAXBElement<GetServerInfo>(_GetServerInfo_QNAME, GetServerInfo.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InfoNotFoundException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://fileserver/", name = "InfoNotFoundException")
    public JAXBElement<InfoNotFoundException> createInfoNotFoundException(InfoNotFoundException value) {
        return new JAXBElement<InfoNotFoundException>(_InfoNotFoundException_QNAME, InfoNotFoundException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AddPermissionResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://fileserver/", name = "addPermissionResponse")
    public JAXBElement<AddPermissionResponse> createAddPermissionResponse(AddPermissionResponse value) {
        return new JAXBElement<AddPermissionResponse>(_AddPermissionResponse_QNAME, AddPermissionResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DownloadFile }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://fileserver/", name = "downloadFile")
    public JAXBElement<DownloadFile> createDownloadFile(DownloadFile value) {
        return new JAXBElement<DownloadFile>(_DownloadFile_QNAME, DownloadFile.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RmResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://fileserver/", name = "rmResponse")
    public JAXBElement<RmResponse> createRmResponse(RmResponse value) {
        return new JAXBElement<RmResponse>(_RmResponse_QNAME, RmResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RemovePermissionResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://fileserver/", name = "removePermissionResponse")
    public JAXBElement<RemovePermissionResponse> createRemovePermissionResponse(RemovePermissionResponse value) {
        return new JAXBElement<RemovePermissionResponse>(_RemovePermissionResponse_QNAME, RemovePermissionResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AddPermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://fileserver/", name = "addPermission")
    public JAXBElement<AddPermission> createAddPermission(AddPermission value) {
        return new JAXBElement<AddPermission>(_AddPermission_QNAME, AddPermission.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RemovePermission }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://fileserver/", name = "removePermission")
    public JAXBElement<RemovePermission> createRemovePermission(RemovePermission value) {
        return new JAXBElement<RemovePermission>(_RemovePermission_QNAME, RemovePermission.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UploadFile }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://fileserver/", name = "uploadFile")
    public JAXBElement<UploadFile> createUploadFile(UploadFile value) {
        return new JAXBElement<UploadFile>(_UploadFile_QNAME, UploadFile.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UploadFileResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://fileserver/", name = "uploadFileResponse")
    public JAXBElement<UploadFileResponse> createUploadFileResponse(UploadFileResponse value) {
        return new JAXBElement<UploadFileResponse>(_UploadFileResponse_QNAME, UploadFileResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetServerInfoResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://fileserver/", name = "getServerInfoResponse")
    public JAXBElement<GetServerInfoResponse> createGetServerInfoResponse(GetServerInfoResponse value) {
        return new JAXBElement<GetServerInfoResponse>(_GetServerInfoResponse_QNAME, GetServerInfoResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Dir }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://fileserver/", name = "dir")
    public JAXBElement<Dir> createDir(Dir value) {
        return new JAXBElement<Dir>(_Dir_QNAME, Dir.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetFileInfoResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://fileserver/", name = "getFileInfoResponse")
    public JAXBElement<GetFileInfoResponse> createGetFileInfoResponse(GetFileInfoResponse value) {
        return new JAXBElement<GetFileInfoResponse>(_GetFileInfoResponse_QNAME, GetFileInfoResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DirResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://fileserver/", name = "dirResponse")
    public JAXBElement<DirResponse> createDirResponse(DirResponse value) {
        return new JAXBElement<DirResponse>(_DirResponse_QNAME, DirResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link byte[]}{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "arg1", scope = UploadFile.class)
    public JAXBElement<byte[]> createUploadFileArg1(byte[] value) {
        return new JAXBElement<byte[]>(_UploadFileArg1_QNAME, byte[].class, UploadFile.class, ((byte[]) value));
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link byte[]}{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "return", scope = DownloadFileResponse.class)
    public JAXBElement<byte[]> createDownloadFileResponseReturn(byte[] value) {
        return new JAXBElement<byte[]>(_DownloadFileResponseReturn_QNAME, byte[].class, DownloadFileResponse.class, ((byte[]) value));
    }

}
