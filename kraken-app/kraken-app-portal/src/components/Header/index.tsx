import {
  Button,
  Card,
  Divider,
  Dropdown,
  Flex,
  Tag,
  Tooltip,
} from "antd";
import styles from "./index.module.scss";
import Logo from "@/assets/logo.svg";
import {
  CloseOutlined,
  EditTwoTone,
  LogoutOutlined,
  QuestionCircleOutlined,
} from "@ant-design/icons";
import { useTutorialStore } from "@/stores/tutorial.store";
import useUser from "@/hooks/user/useUser";
import { Text } from "../Text";
import { Link, useNavigate } from "react-router-dom";
import { UserAvatar } from "./UserAvatar";
import { ISystemInfo } from "@/utils/types/user.type";
import UpgradingIcon from '@/assets/icon/upgrading.svg'

const TooltipBody = (setTutorialCompleted: (value: boolean) => void) => (
  <div className={styles.tooltip}>
    <Flex justify="space-between">
      <span>Open the guide here.</span>
      <CloseOutlined
        style={{ fontSize: 12 }}
        onClick={() => setTutorialCompleted(false)}
      />
    </Flex>
    <Flex justify="end">
      <Button
        onClick={() => {
          setTutorialCompleted(false);
        }}
      >
        Got it
      </Button>
    </Flex>
  </div>
);

const Header = ({ info }: Readonly<{ info?: ISystemInfo }>) => {
  const { currentUser } = useUser();
  const { tutorialCompleted, setTutorialCompleted, setOpenTutorial } =
    useTutorialStore();
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.clear();
    navigate("/login");
  };

  const dropdownRender = () => {
    return (
      <Card
        className={styles.root}
        actions={[
          <Flex
            data-testid="logoutOpt"
            key="log-out"
            align="center"
            gap={10}
            justify="center"
            onClick={handleLogout}
          >
            <LogoutOutlined style={{ color: "#FF3864" }} />
            <Text.LightMedium color="#FF3864">Log Out</Text.LightMedium>
          </Flex>,
        ]}
      >
        <Flex justify="center" align="center" vertical gap={8}>
          <div style={{ position: "relative" }}>
            <UserAvatar size={64} className={styles.avatarLg} user={currentUser} />
            <div className={styles.edit}>
              <EditTwoTone style={{ fontSize: 12 }} />
            </div>
          </div>
          <Text.Custom size="20px" lineHeight="28px" bold="500">
            {currentUser?.name}
          </Text.Custom>
          <Text.LightMedium lineHeight="22px" color="rgba(0, 0, 0, 0.45)">
            {currentUser?.email}
          </Text.LightMedium>
        </Flex>
      </Card>
    );
  };

  return (
    <div className={styles.header}>
      <Flex gap={16} align="center">
        <Link data-testid="logo" to="/" className={styles.logo}>
          <Logo />
          Kraken
        </Link>

        <Divider type="vertical" className={styles.divider} />

        <Text.LightMedium data-testid="productName">{info?.productName || info?.productKey}</Text.LightMedium>
        <Tag data-testid="productSpec" style={{ background: 'var(--bg)', color: 'var(--text-secondary)', border: 'none' }}>{info?.productSpec}</Tag>
      </Flex>

      <div className={styles.rightMenu}>
        {/* This mean the template mapping is in Upgrading process */}
        {info?.status && info.status !== 'RUNNING' && (
          <>
            <Link data-testid="mappingInProgress" to="/mapping-template-v2" className={styles.mappingInProgress}>
              <UpgradingIcon />
              Mapping template upgrading
            </Link>
            <Divider type="vertical" style={{ height: 16, padding: 0}} />
          </>
        )}

        <Tooltip
          align={{ offset: [12, 15] }}
          overlayInnerStyle={{ zIndex: 1 }}
          placement="bottomLeft"
          open={tutorialCompleted || undefined}
          title={TooltipBody(setTutorialCompleted)}
          rootClassName={styles.tooltipBlue}
        >
          <QuestionCircleOutlined onClick={() => setOpenTutorial(true)} />
        </Tooltip>

        <Dropdown
          placement="bottomRight"
          className={styles.avatar}
          menu={{ items: [] }}
          dropdownRender={dropdownRender}
        >
          <UserAvatar size={24} user={currentUser} />
        </Dropdown>
      </div>
    </div>
  );
};

export default Header;
