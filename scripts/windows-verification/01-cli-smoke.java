import com.fs.aicommit.SdkDefinition;
import com.fs.aicommit.SdkManager;
import com.fs.aicommit.SdkStatus;

class CliSmoke {
    public static void main(String[] args) {
        for (SdkDefinition sdk : SdkDefinition.values()) {
            SdkStatus status = SdkManager.INSTANCE.detect(sdk, null);
            System.out.printf("%s: installed=%s, version=%s, path=%s%n",
                sdk.getDisplayName(), status.getInstalled(), status.getVersion(), status.getPath());
        }
    }
}
